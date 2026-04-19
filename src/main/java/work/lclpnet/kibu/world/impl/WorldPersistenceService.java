package work.lclpnet.kibu.world.impl;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.worldupdate.UpgradeProgress;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.validation.ContentValidationException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import work.lclpnet.kibu.world.KibuLevelConfig;
import work.lclpnet.kibu.world.mixin.MinecraftServerAccessor;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

@ApiStatus.Internal
public class WorldPersistenceService {

    private final MinecraftServer server;
    private final Logger logger;

    public WorldPersistenceService(MinecraftServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    public Optional<RuntimeLevelHandle> tryRecreateWorld(Identifier identifier) {
        ResourceKey<Level> registryKey = ResourceKey.create(Registries.DIMENSION, identifier);

        Fantasy fantasy = Fantasy.get(server);
        ServerLevel world = server.getLevel(registryKey);

        if (world != null) {
            // world exists, it is safe to call getOrOpenPersistentLevel()
            RuntimeLevelHandle handle = fantasy.getOrOpenPersistentLevel(identifier, null);
            return Optional.of(handle);
        }

        // try to restore config
        RuntimeLevelConfig config;

        try {
            config = restoreConfig(registryKey);
        } catch (Throwable t) {
            logger.error("Failed to restore runtime world config for world {}", identifier, t);
            return Optional.empty();
        }

        if (config == null) {
            return Optional.empty();
        }

        // config restored successfully
        RuntimeLevelHandle handle = fantasy.getOrOpenPersistentLevel(identifier, config);

        return Optional.of(handle);
    }

    @Nullable
    public RuntimeLevelConfig restoreConfig(ResourceKey<Level> registryKey) {
        Result persistedData = readPersistedData(registryKey);

        WorldGenSettings worldGenSettings = persistedData.worldGenSettings().orElse(null);

        if (worldGenSettings == null) {
            logger.warn("Cannot restore level config of a level that wasn't saved with kibu-world-api installed");
            return null;
        }

        LevelStem dimension = findMainDimension(worldGenSettings.dimensions());

        if (dimension == null) {
            logger.error("Could not find main dimension for level {}", registryKey);
            return null;
        }

        RuntimeLevelConfig config = new RuntimeLevelConfig()
                .setDimensionType(dimension.type())
                .setGenerator(dimension.generator())
                .setSeed(worldGenSettings.options().seed());

        // primary level data from level.dat in the dimension directory is written by kibu-world-api
        @Nullable PrimaryLevelData primaryLevelData = readPrimaryLevelData(registryKey, worldGenSettings);

        if (primaryLevelData != null) {
            ((KibuLevelConfig) (Object) config).kibu$setPrimaryLevelData(primaryLevelData);

            config
                    .setFlat(primaryLevelData.isFlatWorld())
                    .setDifficulty(primaryLevelData.getDifficulty())
                    .setGameTime(primaryLevelData.getGameTime());
        }


        // TODO
//        config.setSunny(properties.getClearWeatherTime());
//        config.setRaining(properties.getRainTime());
//        config.setRaining(properties.isRaining());
//        config.setThundering(properties.isThundering());
//        config.setThundering(properties.getThunderTime());
//        config.setTimeOfDay(properties.getDayTime());

        persistedData.gameRuleMap().ifPresent(gameRuleMap -> {
            for (GameRule<?> rule : gameRuleMap.keySet()) {
                var value = gameRuleMap.get(rule);

                if (value == null) continue;

                assign(config, rule, value);
            }
        });

        Object shouldTickTime = config.getGameRules().get(GameRules.ADVANCE_TIME);

        if (shouldTickTime instanceof Boolean b) {
            config.setShouldTickTime(b);
        }

        return config;
    }

    @SuppressWarnings("unchecked")
    private <T> void assign(RuntimeLevelConfig config, GameRule<T> rule, Object value) {
        config.setGameRule(rule, (T) value);
    }

    @Nullable
    private LevelStem findMainDimension(WorldDimensions worldDimensions) {
        Map<ResourceKey<LevelStem>, LevelStem> dimensions = worldDimensions.dimensions();

        LevelStem overworld = dimensions.get(LevelStem.OVERWORLD);

        if (overworld != null) {
            return overworld;
        }

        // there is no overworld entry, accept any other dimension
        var iterator = dimensions.values().iterator();

        if (iterator.hasNext()) {
            return iterator.next();
        }

        return null;
    }

    private WorldPersistenceService.Result readPersistedData(ResourceKey<Level> registryKey) {
        LevelStorageSource.LevelStorageAccess access = ((MinecraftServerAccessor) server).getStorageSource();

        Path path = access.getDimensionPath(registryKey);
        RegistryAccess.Frozen registryManager = server.registries().compositeAccess();

        Path dataPath = path.resolve("data");
        DataFixer fixerUpper = server.getFixerUpper();

        try (var storage = new SavedDataStorage(dataPath, fixerUpper, registryManager)) {
            @Nullable WorldGenSettings worldGenSettings = storage.get(WorldGenSettings.TYPE);
            @Nullable GameRuleMap gameRuleMap = storage.get(GameRuleMap.TYPE);
            @Nullable WeatherData weatherData = storage.get(WeatherData.TYPE);

            return new Result(
                    Optional.ofNullable(worldGenSettings),
                    Optional.ofNullable(gameRuleMap),
                    Optional.ofNullable(weatherData)
            );
        }
    }

    @Nullable
    private PrimaryLevelData readPrimaryLevelData(ResourceKey<Level> registryKey, WorldGenSettings worldGenSettings) {
        Path worldDir = getWorldDirectory(registryKey);

        var registryManager = server.registries().compositeAccess();
        DataFixer dataFixer = server.getFixerUpper();

        LevelStorageSource levelStorageSource = LevelStorageSource.createDefault(worldDir.getParent());

        String name = worldDir.getFileName().toString();

        // from net.minecraft.server.Main.main
        try (LevelStorageSource.LevelStorageAccess access = levelStorageSource.validateAndCreateAccess(name)) {
            if (!access.hasWorldData()) {
                return null;
            }

            Dynamic<?> levelDataUnfixed;

            try {
                levelDataUnfixed = access.getUnfixedDataTagWithFallback();
            } catch (NbtException | ReportedNbtException | IOException var39) {
                logger.error("Failed to load world data. World files may be corrupted.", var39);
                return null;
            }

            LevelSummary summary = access.fixAndGetSummaryFromTag(levelDataUnfixed);
            if (summary.requiresManualConversion()) {
                logger.info("This world must be opened in an older version (like 1.6.4) to be safely converted");
                return null;
            }

            if (!summary.isCompatible()) {
                logger.info("This world was created by an incompatible version.");
                return null;
            }

            Dynamic<?> levelDataTag = DataFixers.getFileFixer().fix(access, levelDataUnfixed, new UpgradeProgress());

            return readPrimaryLevelDataFromTag(worldGenSettings, levelDataTag, registryManager, dataFixer);
        } catch (IOException | ContentValidationException e) {
            throw new RuntimeException(e);
        }
    }

    private @NonNull PrimaryLevelData readPrimaryLevelDataFromTag(
            WorldGenSettings worldGenSettings,
            Dynamic<?> levelDataTag,
            RegistryAccess.Frozen registryManager,
            DataFixer dataFixer
    ) {
        // adapted from net.minecraft.world.level.storage.LevelStorageSource.getLevelDataAndDimensions
        Dynamic<?> dataTag = RegistryOps.injectRegistryContext(levelDataTag, registryManager);

        Lifecycle registryLifecycle = registryManager.allRegistriesLifecycle();

        // use an empty registry to only read the entries from the nbt
        Registry<LevelStem> existingDimOptions = new MappedRegistry<>(Registries.LEVEL_STEM, registryLifecycle);
        WorldDimensions.Complete dimensions = worldGenSettings.dimensions().bake(existingDimOptions);

        WorldDataConfiguration dataConfiguration = getDataConfiguration(dataTag, dataFixer);
        LevelSettings settings = LevelSettings.parse(dataTag, dataConfiguration);

        Lifecycle lifecycle = dimensions.lifecycle().add(registryLifecycle);

        return PrimaryLevelData.parse(dataTag, settings, dimensions.specialWorldProperty(), lifecycle);
    }

    @NotNull
    private WorldDataConfiguration getDataConfiguration(Dynamic<?> data, DataFixer dataFixer) {
        int dataVersion = NbtUtils.getDataVersion(data, -1);

        Dynamic<?> dynamic = DataFixTypes.LEVEL.updateToCurrentVersion(dataFixer, data, dataVersion);

        return WorldDataConfiguration.CODEC.parse(dynamic)
                .resultOrPartial(logger::error)
                .orElse(WorldDataConfiguration.DEFAULT);
    }

    @NotNull
    private Path getWorldDirectory(ResourceKey<Level> registryKey) {
        LevelStorageSource.LevelStorageAccess access = ((MinecraftServerAccessor) server).getStorageSource();

        return access.getDimensionPath(registryKey);
    }

    private record Result(
            Optional<WorldGenSettings> worldGenSettings,
            Optional<GameRuleMap> gameRuleMap,
            Optional<WeatherData> weatherData
    ) {}
}
