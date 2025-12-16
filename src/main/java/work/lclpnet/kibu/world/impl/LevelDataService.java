package work.lclpnet.kibu.world.impl;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import work.lclpnet.kibu.world.data.LevelDataDeserializer;
import work.lclpnet.kibu.world.data.LevelDataSerializer;
import work.lclpnet.kibu.world.mixin.PrimaryLevelDataAccessor;
import xyz.nucleoid.fantasy.RuntimeWorld;

import java.util.Optional;
import java.util.stream.Stream;

@ApiStatus.Internal
public class LevelDataService implements LevelDataSerializer, LevelDataDeserializer {

    private final Logger logger;

    public LevelDataService(Logger logger) {
        this.logger = logger;
    }

    @Override
    public CompoundTag serializeLevelData(ServerLevel world) {
        PrimaryLevelData saveProperties = getLevelProperties(world);

        RegistryAccess registryManager = getCustomRegistryManager(world);

        CompoundTag data = saveProperties.createTag(registryManager, null);

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
        LevelData worldProps = world.getLevelData();

        // runtime worlds do not store generator options in the level properties atm
        if (world instanceof RuntimeWorld runtimeWorld) {
            PrimaryLevelDataAccessor writer = (PrimaryLevelDataAccessor) props;

            // copy generator options here
            WorldOptions genOpts = new WorldOptions(
                    runtimeWorld.getSeed(),
                    runtimeWorld.structureManager().shouldGenerateStructures(),
                    false
            );

            writer.setWorldOptions(genOpts);
        }

        // vanilla encapsulation
        props.setDifficulty(worldProps.getDifficulty());
        props.setSpawn(worldProps.getRespawnData());
        props.setGameTime(worldProps.getGameTime());
        props.setDayTime(worldProps.getDayTime());
        props.setDifficultyLocked(worldProps.isDifficultyLocked());
        props.setRaining(worldProps.isRaining());
        props.setThundering(worldProps.isThundering());

        if (worldProps instanceof ServerLevelData swProps) {
            props.getGameRules().assignFrom(swProps.getGameRules(), null);
            props.setClearWeatherTime(swProps.getClearWeatherTime());
            props.setRainTime(swProps.getRainTime());
            props.setThunderTime(swProps.getThunderTime());
            props.setGameType(swProps.getGameType());
            props.setInitialized(swProps.isInitialized());
            props.setWanderingTraderId(swProps.getWanderingTraderId());
            props.setWanderingTraderSpawnChance(swProps.getWanderingTraderSpawnChance());
            props.setWanderingTraderSpawnDelay(swProps.getWanderingTraderSpawnDelay());
            props.setLegacyWorldBorderSettings(swProps.getLegacyWorldBorderSettings());
        }

        return props;
    }

    /**
     * Clones given {@link WorldData} and narrows the type to {@link PrimaryLevelData}.
     * @param properties The save properties to clone.
     * @return Cloned properties as level properties.
     */
    private PrimaryLevelData cloneSaveProperties(MinecraftServer server, WorldData properties) {
        CompoundTag data = properties.createTag(server.registryAccess(), null);

        CompoundTag nbt = new CompoundTag();
        nbt.put("Data", data);

        return deserializeLevelData(nbt, server).properties();
    }

    /**
     * Create a custom registry manager that replaces the dimension option registry.
     * The dimension option registry only contains the dimension option of the given world.
     * All other registries are taken from the combined dynamic registries of the server.
     * @param world The world to put the dimension options of.
     * @return A {@link RegistryAccess} with replaced dimension options registry.
     */
    @NotNull
    private static RegistryAccess getCustomRegistryManager(ServerLevel world) {
        MinecraftServer server = world.getServer();

        var parent = server.registries().compositeAccess();

        var registry = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable());

        ChunkGenerator chunkGenerator = world.getChunkSource().getGenerator();
        Holder<DimensionType> dimensionType = world.dimensionTypeRegistration();
        LevelStem dimensionOptions = new LevelStem(dimensionType, chunkGenerator);

        registry.register(LevelStem.OVERWORLD, dimensionOptions, RegistrationInfo.BUILT_IN);

        return new RegistryAccess() {
            @SuppressWarnings("unchecked")
            @Override
            public <E> Optional<Registry<E>> lookup(ResourceKey<? extends Registry<? extends E>> key) {
                if (Registries.LEVEL_STEM.equals(key)) {
                    return Optional.of((Registry<E>) registry);
                }

                return parent.lookup(key);
            }

            @Override
            public Stream<RegistryEntry<?>> registries() {
                return Stream.concat(
                        Stream.of(new RegistryEntry<>(Registries.LEVEL_STEM, registry)),
                        parent.registries().filter(entry -> !Registries.LEVEL_STEM.equals(entry.key()))
                );
            }
        };
    }

    @Override
    public LevelDataDeserializer.Result deserializeLevelData(CompoundTag levelData, MinecraftServer server) {
        var registryManager = server.registries().compositeAccess();
        DataFixer dataFixer = server.getFixerUpper();
        Lifecycle registryLifecycle = registryManager.allRegistriesLifecycle();

        var levelDynamic = getLevelProperties(levelData, dataFixer);
        var dynamic = wrap(levelDynamic, registryManager);

        WorldGenSettings worldGenSettings = getWorldGenSettings(dynamic);

        CompoundTag data = levelData.getCompoundOrEmpty("Data");
        WorldDataConfiguration dataConfiguration = getDataConfiguration(data, dataFixer);
        LevelSettings levelInfo = LevelSettings.parse(dynamic, dataConfiguration);

        // use an empty registry to only read the entries from the nbt
        Registry<LevelStem> existingDimOptions = new MappedRegistry<>(Registries.LEVEL_STEM, registryLifecycle);

        var dimensionsConfig = worldGenSettings.dimensions()
                .bake(existingDimOptions);

        Lifecycle propsLifecycle = dimensionsConfig.lifecycle().add(registryLifecycle);

        PrimaryLevelData levelProperties = PrimaryLevelData.parse(dynamic, levelInfo,
                dimensionsConfig.specialWorldProperty(), worldGenSettings.options(), propsLifecycle);

        return new Result(levelProperties, dimensionsConfig);
    }

    private WorldGenSettings getWorldGenSettings(Dynamic<Tag> dynamic) {
        var worldGenSettingsDynamic = dynamic.get("WorldGenSettings").orElseEmptyMap();

        return WorldGenSettings.CODEC.parse(worldGenSettingsDynamic).getOrThrow();
    }

    // from net.minecraft.world.level.storage.LevelStorage.readLevelProperties(java.nio.file.Path, com.mojang.datafixers.DataFixer)
    // adjusted so that NbtCompound is used instead
    private Dynamic<Tag> getLevelProperties(CompoundTag nbtCompound, DataFixer dataFixer) {
        CompoundTag data = nbtCompound.getCompoundOrEmpty("Data");
        int dataVersion = NbtUtils.getDataVersion(data, -1);

        Dynamic<Tag> levelProps = DataFixTypes.LEVEL.updateToCurrentVersion(dataFixer, new Dynamic<>(NbtOps.INSTANCE, data), dataVersion);

        Dynamic<Tag> player = levelProps.get("Player").orElseEmptyMap();
        Dynamic<Tag> updatedPlayer = DataFixTypes.PLAYER.updateToCurrentVersion(dataFixer, player, dataVersion);
        levelProps = levelProps.set("Player", updatedPlayer);

        Dynamic<Tag> worldGenSettings = levelProps.get("WorldGenSettings").orElseEmptyMap();
        Dynamic<Tag> updatedWorldGenSettings = DataFixTypes.WORLD_GEN_SETTINGS.updateToCurrentVersion(dataFixer, worldGenSettings, dataVersion);
        levelProps = levelProps.set("WorldGenSettings", updatedWorldGenSettings);

        return levelProps;
    }

    private <T> Dynamic<T> wrap(Dynamic<T> dynamic, RegistryAccess.Frozen registryManager) {
        RegistryOps<T> registryOps = RegistryOps.create(dynamic.getOps(), registryManager);
        return new Dynamic<>(registryOps, dynamic.getValue());
    }

    @NotNull
    private WorldDataConfiguration getDataConfiguration(CompoundTag data, DataFixer dataFixer) {
        int dataVersion = NbtUtils.getDataVersion(data, -1);

        Dynamic<Tag> dataDynamic = new Dynamic<>(NbtOps.INSTANCE, data);
        Dynamic<Tag> dynamic = DataFixTypes.LEVEL.updateToCurrentVersion(dataFixer, dataDynamic, dataVersion);

        return WorldDataConfiguration.CODEC.parse(dynamic)
                .resultOrPartial(logger::error)
                .orElse(WorldDataConfiguration.DEFAULT);
    }
}
