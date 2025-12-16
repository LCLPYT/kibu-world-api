package work.lclpnet.kibu.world.impl;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
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

    public Optional<RuntimeWorldHandle> tryRecreateWorld(ResourceLocation identifier) {
        ResourceKey<Level> registryKey = ResourceKey.create(Registries.DIMENSION, identifier);

        Fantasy fantasy = Fantasy.get(server);
        ServerLevel world = server.getLevel(registryKey);

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
    public RuntimeWorldConfig restoreConfig(ResourceKey<Level> registryKey) {
        // try to read levelData
        LevelDataDeserializer.Result levelData = readLevelData(registryKey);

        if (levelData == null) {
            return null;
        }

        PrimaryLevelData properties = levelData.properties();
        WorldDimensions.Complete dimensionsConfig = levelData.dimensions();

        LevelStem dimension = findMainDimension(dimensionsConfig);

        if (dimension == null) {
            logger.error("Could not find main dimension for level {}", properties.getLevelName());
            return null;
        }

        RuntimeWorldConfig config = new RuntimeWorldConfig()
                .setDimensionType(dimension.type())
                .setGenerator(dimension.generator())
                .setFlat(properties.isFlatWorld())
                .setDifficulty(properties.getDifficulty());

        WorldOptions generatorOptions = properties.worldGenOptions();
        config.setSeed(generatorOptions.seed());

        config.setSunny(properties.getClearWeatherTime());
        config.setRaining(properties.getRainTime());
        config.setRaining(properties.isRaining());
        config.setThundering(properties.isThundering());
        config.setThundering(properties.getThunderTime());
        config.setTimeOfDay(properties.getDayTime());

        GameRules gameRules = properties.getGameRules();
        Map<GameRules.Key<?>, GameRules.Value<?>> ruleMap = ((GameRuleAccess) gameRules).kibu$getRules();

        ruleMap.forEach((key, rule) -> {
            if (rule instanceof GameRules.BooleanValue booleanRule) {
                config.setGameRule(cast(key), booleanRule.get());
            } else if (rule instanceof GameRules.IntegerValue intRule) {
                config.setGameRule(cast(key), intRule.get());
            }
        });

        config.setShouldTickTime(config.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT));

        return config;
    }

    @SuppressWarnings("unchecked")
    private static <T extends GameRules.Value<T>> GameRules.Key<T> cast(GameRules.Key<?> key) {
        return (GameRules.Key<T>) key;
    }

    @Nullable
    private LevelStem findMainDimension(WorldDimensions.Complete config) {
        Registry<LevelStem> dimensions = config.dimensions();

        if (dimensions.containsKey(LevelStem.OVERWORLD)) {
            LevelStem overworld = dimensions.getValue(LevelStem.OVERWORLD);

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
    private LevelDataDeserializer.Result readLevelData(ResourceKey<Level> registryKey) {
        Path directory = getWorldDirectory(registryKey);
        Path levelDat = directory.resolve(LevelResource.LEVEL_DATA_FILE.getId());

        if (!Files.exists(levelDat)) {
            logger.warn("Level data file does not exist at {}", levelDat);
            return null;
        }

        CompoundTag nbt;

        try (var in = Files.newInputStream(levelDat)) {
            nbt = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            logger.error("Failed to read compressed nbt from {}", levelDat, e);
            return null;
        }

        return dataReader.deserializeLevelData(nbt, server);
    }

    @NotNull
    private Path getWorldDirectory(ResourceKey<Level> registryKey) {
        LevelStorageSource.LevelStorageAccess session = ((MinecraftServerAccessor) server).getStorageSource();

        return session.getDimensionPath(registryKey);
    }
}
