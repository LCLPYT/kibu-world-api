package work.lclpnet.kibu.world.impl;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import work.lclpnet.kibu.world.data.LevelDataDeserializer;
import work.lclpnet.kibu.world.data.LevelDataSerializer;

import java.util.Map;

@ApiStatus.Internal
public class LevelDataService implements LevelDataSerializer, LevelDataDeserializer {

    private final Logger logger;

    public LevelDataService(Logger logger) {
        this.logger = logger;
    }

    @Override
    public CompoundTag serializeLevelData(ServerLevel level) {
        PrimaryLevelData saveProperties = getLevelProperties(level);

        CompoundTag data = saveProperties.createTag(null);

        CompoundTag nbt = new CompoundTag();
        nbt.put("Data", data);

        return nbt;
    }

    /**
     * This method creates a {@link PrimaryLevelData} object for a given {@link ServerLevel}.
     * The {@link WorldData} of the {@link MinecraftServer} are used as base.
     * @param world The world.
     * @return The {@link PrimaryLevelData} that can be used to save the world to disk.
     */
    private PrimaryLevelData getLevelProperties(ServerLevel world) {
        // use save properties of the server as parent (create a copy)
        MinecraftServer server = world.getServer();
        WorldData parent = world.getServer().getWorldData();
        PrimaryLevelData props = cloneSaveProperties(server, parent);

        // now set the actual data of the world
        LevelData levelData = world.getLevelData();

        props.setDifficulty(levelData.getDifficulty());
        props.setSpawn(levelData.getRespawnData());
        props.setGameTime(levelData.getGameTime());
        props.setDifficultyLocked(levelData.isDifficultyLocked());
        props.setGameTime(levelData.getGameTime());

        // TODO
//        props.setRaining(levelData.isRaining());
//        props.setThundering(levelData.isThundering());

        if (levelData instanceof ServerLevelData swProps) {
            props.setGameType(swProps.getGameType());
            props.setInitialized(swProps.isInitialized());
        }

        return props;
    }

    /**
     * Clones given {@link WorldData} and narrows the type to {@link PrimaryLevelData}.
     * @param properties The save properties to clone.
     * @return Cloned properties as level properties.
     */
    private PrimaryLevelData cloneSaveProperties(MinecraftServer server, WorldData properties) {
        CompoundTag data = properties.createTag(null);

        CompoundTag nbt = new CompoundTag();
        nbt.put("Data", data);

        var registryManager = server.registries().compositeAccess();
        var dataTag = new Dynamic<>(registryManager.createSerializationContext(NbtOps.INSTANCE), nbt);

        WorldGenSettings worldGenSettings = server.getWorldGenSettings();

        return deserializePrimaryLevelData(worldGenSettings, dataTag, server);
    }

    @Override
    public @NonNull PrimaryLevelData deserializePrimaryLevelData(WorldGenSettings worldGenSettings, Dynamic<?> levelDataTag, MinecraftServer server) {
        var registryManager = server.registries().compositeAccess();
        DataFixer dataFixer = server.getFixerUpper();

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

    public void writeCustomLevelData(ServerLevel level) {
        WorldGenSettings worldGenSettings = getWorldGenSettings(level);
        GameRuleMap gameRuleMap = getGameRuleMap(level);

        SavedDataStorage dataStorage = level.getDataStorage();
        dataStorage.set(WorldGenSettings.TYPE, worldGenSettings);
        dataStorage.set(GameRuleMap.TYPE, gameRuleMap);
        // TODO weather

        dataStorage.saveAndJoin();
    }

    @NotNull
    private GameRuleMap getGameRuleMap(ServerLevel level) {
        GameRuleMap map = GameRuleMap.of();

        GameRules levelGameRules = level.getGameRules();

        levelGameRules.availableRules().forEach(gameRule -> {
            Object value = levelGameRules.get(gameRule);

            assignGameRule(map, gameRule, value);
        });

        return map;
    }

    @SuppressWarnings("unchecked")
    private <T> void assignGameRule(GameRuleMap map, GameRule<T> rule, Object value) {
        map.set(rule, (T) value);
    }

    private @NonNull WorldGenSettings getWorldGenSettings(ServerLevel level) {
        WorldGenSettings serverWorldGenSettings = level.getServer().getDataStorage().get(WorldGenSettings.TYPE);

        WorldOptions options = new WorldOptions(
                level.getSeed(),
                serverWorldGenSettings == null || serverWorldGenSettings.options().generateStructures(),
                serverWorldGenSettings != null && serverWorldGenSettings.options().generateBonusChest()
        );

        ChunkGenerator chunkGenerator = level.getChunkSource().getGenerator();
        Holder<DimensionType> dimensionType = level.dimensionTypeRegistration();
        LevelStem dimensionOptions = new LevelStem(dimensionType, chunkGenerator);

        WorldDimensions dimensions = new WorldDimensions(Map.of(LevelStem.OVERWORLD, dimensionOptions));

        return new WorldGenSettings(options, dimensions);
    }
}
