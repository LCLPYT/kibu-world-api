package work.lclpnet.kibu.world.impl;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import work.lclpnet.kibu.world.GameRuleAccess;
import work.lclpnet.kibu.world.data.LevelDataDeserializer;
import work.lclpnet.kibu.world.mixin.MinecraftServerAccessor;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

@ApiStatus.Internal
public class WorldPersistenceService {

    private final MinecraftServer server;
    private final LevelDataDeserializer dataReader;
    private final Logger logger;

    public WorldPersistenceService(MinecraftServer server, LevelDataDeserializer dataReader, Logger logger) {
        this.server = server;
        this.dataReader = dataReader;
        this.logger = logger;
    }

    public Optional<RuntimeWorldHandle> tryRecreateWorld(Identifier identifier) {
        RegistryKey<World> registryKey = RegistryKey.of(RegistryKeys.WORLD, identifier);

        Fantasy fantasy = Fantasy.get(server);
        ServerWorld world = server.getWorld(registryKey);

        if (world != null) {
            // world exists, it is safe to call getOrOpenPersistentWorld()
            RuntimeWorldHandle handle = fantasy.getOrOpenPersistentWorld(identifier, null);
            return Optional.of(handle);
        }

        // try to restore config
        RuntimeWorldConfig config;

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
        RuntimeWorldHandle handle = fantasy.getOrOpenPersistentWorld(identifier, config);

        return Optional.of(handle);
    }

    @Nullable
    public RuntimeWorldConfig restoreConfig(RegistryKey<World> registryKey) {
        // try to read levelData
        LevelDataDeserializer.Result levelData = readLevelData(registryKey);

        if (levelData == null) {
            return null;
        }

        LevelProperties properties = levelData.properties();
        DimensionOptionsRegistryHolder.DimensionsConfig dimensionsConfig = levelData.dimensions();

        DimensionOptions dimension = findMainDimension(dimensionsConfig);

        if (dimension == null) {
            logger.error("Could not find main dimension for level {}", properties.getLevelName());
            return null;
        }

        RuntimeWorldConfig config = new RuntimeWorldConfig()
                .setDimensionType(dimension.dimensionTypeEntry())
                .setGenerator(dimension.chunkGenerator())
                .setFlat(properties.isFlatWorld())
                .setDifficulty(properties.getDifficulty());

        GeneratorOptions generatorOptions = properties.getGeneratorOptions();
        config.setSeed(generatorOptions.getSeed());

        config.setRaining(properties.getRainTime());
        config.setRaining(properties.isRaining());
        config.setSunny(properties.getClearWeatherTime());
        config.setThundering(properties.isThundering());
        config.setThundering(properties.getThunderTime());
        config.setTimeOfDay(properties.getTimeOfDay());

        GameRules gameRules = properties.getGameRules();
        Map<GameRules.Key<?>, GameRules.Rule<?>> ruleMap = ((GameRuleAccess) gameRules).kibu$getRules();

        ruleMap.forEach((key, rule) -> {
            if (rule instanceof GameRules.BooleanRule booleanRule) {
                config.setGameRule(cast(key), booleanRule.get());
            } else if (rule instanceof GameRules.IntRule intRule) {
                config.setGameRule(cast(key), intRule.get());
            }
        });

        return config;
    }

    @SuppressWarnings("unchecked")
    private static <T extends GameRules.Rule<T>> GameRules.Key<T> cast(GameRules.Key<?> key) {
        return (GameRules.Key<T>) key;
    }

    @Nullable
    private DimensionOptions findMainDimension(DimensionOptionsRegistryHolder.DimensionsConfig config) {
        Registry<DimensionOptions> dimensions = config.dimensions();

        if (dimensions.contains(DimensionOptions.OVERWORLD)) {
            DimensionOptions overworld = dimensions.get(DimensionOptions.OVERWORLD);

            if (overworld != null) {
                return overworld;
            }
        }

        // there is no overworld entry, accept any other dimension
        var iterator = dimensions.iterator();

        if (iterator.hasNext()) {
            return iterator.next();
        }

        return null;
    }

    @Nullable
    private LevelDataDeserializer.Result readLevelData(RegistryKey<World> registryKey) {
        Path directory = getWorldDirectory(registryKey);
        Path levelDat = directory.resolve(WorldSavePath.LEVEL_DAT.getRelativePath());

        if (!Files.exists(levelDat)) {
            logger.warn("Level data file does not exist at {}", levelDat);
            return null;
        }

        NbtCompound nbt;

        try (var in = Files.newInputStream(levelDat)) {
            nbt = NbtIo.readCompressed(in, NbtSizeTracker.ofUnlimitedBytes());
        } catch (IOException e) {
            logger.error("Failed to read compressed nbt from {}", levelDat, e);
            return null;
        }

        return dataReader.deserializeLevelData(nbt, server);
    }

    @NotNull
    private Path getWorldDirectory(RegistryKey<World> registryKey) {
        LevelStorage.Session session = ((MinecraftServerAccessor) server).getSession();

        return session.getWorldDirectory(registryKey);
    }
}
