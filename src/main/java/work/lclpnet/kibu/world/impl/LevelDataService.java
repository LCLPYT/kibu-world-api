package work.lclpnet.kibu.world.impl;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.level.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import work.lclpnet.kibu.world.data.LevelDataDeserializer;
import work.lclpnet.kibu.world.data.LevelDataSerializer;
import work.lclpnet.kibu.world.mixin.LevelPropertiesAccessor;
import work.lclpnet.kibu.world.mixin.UnmodifiableLevelPropertiesAccessor;
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
    public NbtCompound serializeLevelData(ServerWorld world) {
        LevelProperties saveProperties = getLevelProperties(world);

        DynamicRegistryManager registryManager = getCustomRegistryManager(world);

        NbtCompound data = saveProperties.cloneWorldNbt(registryManager, null);

        NbtCompound nbt = new NbtCompound();
        nbt.put("Data", data);

        return nbt;
    }

    /**
     * This method creates a {@link LevelProperties} object for a given {@link ServerWorld}.
     * The {@link SaveProperties} of the {@link MinecraftServer} are used as base.
     * @param world The world.
     * @return The {@link LevelProperties} that can be used to save the world to disk.
     */
    private LevelProperties getLevelProperties(ServerWorld world) {
        // use save properties of the server as parent (create a copy)
        MinecraftServer server = world.getServer();
        SaveProperties parent = world.getServer().getSaveProperties();
        LevelProperties props = cloneSaveProperties(server, parent);

        // now set the actual data of the world
        WorldProperties worldProps = world.getLevelProperties();

        // set data for special subtypes first, as some is overwritten by mixins
        if (worldProps instanceof LevelProperties lvlProps) {
            applyLevelProps(lvlProps, props);
        } else if (worldProps instanceof UnmodifiableLevelProperties immutableProps) {
            SaveProperties saveProperties = ((UnmodifiableLevelPropertiesAccessor) immutableProps).getSaveProperties();

            if (saveProperties instanceof LevelProperties lvlProps) {
                applyLevelProps(lvlProps, props);
            }
        }

        // runtime worlds do not store generator options in the level properties atm
        if (world instanceof RuntimeWorld runtimeWorld) {
            LevelPropertiesAccessor writer = (LevelPropertiesAccessor) props;

            // copy generator options here
            GeneratorOptions genOpts = new GeneratorOptions(
                    runtimeWorld.getSeed(),
                    runtimeWorld.getStructureAccessor().shouldGenerateStructures(),
                    false
            );

            writer.setGeneratorOptions(genOpts);
        }

        // vanilla encapsulation
        props.setDifficulty(worldProps.getDifficulty());
        props.setSpawnPos(worldProps.getSpawnPos(), worldProps.getSpawnAngle());
        props.setTime(worldProps.getTime());
        props.setTimeOfDay(worldProps.getTimeOfDay());
        props.setDifficultyLocked(worldProps.isDifficultyLocked());
        props.setRaining(worldProps.isRaining());
        props.setThundering(worldProps.isThundering());

        props.getGameRules().setAllValues(worldProps.getGameRules(), null);

        if (worldProps instanceof ServerWorldProperties swProps) {
            props.setClearWeatherTime(swProps.getClearWeatherTime());
            props.setRainTime(swProps.getRainTime());
            props.setThunderTime(swProps.getThunderTime());
            props.setGameMode(swProps.getGameMode());
            props.setInitialized(swProps.isInitialized());
            props.setWanderingTraderId(swProps.getWanderingTraderId());
            props.setWanderingTraderSpawnChance(swProps.getWanderingTraderSpawnChance());
            props.setWanderingTraderSpawnDelay(swProps.getWanderingTraderSpawnDelay());
            props.setWorldBorder(swProps.getWorldBorder());
        }

        return props;
    }

    private void applyLevelProps(LevelProperties source, LevelProperties dest) {
        dest.setCustomBossEvents(source.getCustomBossEvents());
        dest.setDragonFight(source.getDragonFight());

        // modify inaccessible fields with mixin accessor
        LevelPropertiesAccessor reader = (LevelPropertiesAccessor) source;
        LevelPropertiesAccessor writer = (LevelPropertiesAccessor) dest;

        writer.setLevelInfo(reader.getLevelInfo());
        writer.setGeneratorOptions(source.getGeneratorOptions());
        writer.setSpecialProperty(reader.getSpecialProperty());
        writer.setScheduledEvents(source.getScheduledEvents());
        writer.setRemovedFeatures(source.getRemovedFeatures());
    }

    /**
     * Clones given {@link SaveProperties} and narrows the type to {@link LevelProperties}.
     * @param properties The save properties to clone.
     * @return Cloned properties as level properties.
     */
    private LevelProperties cloneSaveProperties(MinecraftServer server, SaveProperties properties) {
        NbtCompound data = properties.cloneWorldNbt(server.getRegistryManager(), null);

        NbtCompound nbt = new NbtCompound();
        nbt.put("Data", data);

        return deserializeLevelData(nbt, server).properties();
    }

    /**
     * Create a custom registry manager that replaces the dimension option registry.
     * The dimension option registry only contains the dimension option of the given world.
     * All other registries are taken from the combined dynamic registries of the server.
     * @param world The world to put the dimension options of.
     * @return A {@link DynamicRegistryManager} with replaced dimension options registry.
     */
    @NotNull
    private static DynamicRegistryManager getCustomRegistryManager(ServerWorld world) {
        MinecraftServer server = world.getServer();

        var parent = server.getCombinedDynamicRegistries().getCombinedRegistryManager();

        var registry = new SimpleRegistry<>(RegistryKeys.DIMENSION, Lifecycle.stable());

        ChunkGenerator chunkGenerator = world.getChunkManager().getChunkGenerator();
        RegistryEntry<DimensionType> dimensionType = world.getDimensionEntry();
        DimensionOptions dimensionOptions = new DimensionOptions(dimensionType, chunkGenerator);

        registry.add(DimensionOptions.OVERWORLD, dimensionOptions, RegistryEntryInfo.DEFAULT);

        return new DynamicRegistryManager() {
            @SuppressWarnings("unchecked")
            @Override
            public <E> Optional<Registry<E>> getOptional(RegistryKey<? extends Registry<? extends E>> key) {
                if (RegistryKeys.DIMENSION.equals(key)) {
                    return Optional.of((Registry<E>) registry);
                }

                return parent.getOptional(key);
            }

            @Override
            public Stream<Entry<?>> streamAllRegistries() {
                return Stream.concat(
                        Stream.of(new Entry<>(RegistryKeys.DIMENSION, registry)),
                        parent.streamAllRegistries().filter(entry -> !RegistryKeys.DIMENSION.equals(entry.key()))
                );
            }
        };
    }

    @Override
    public LevelDataDeserializer.Result deserializeLevelData(NbtCompound levelData, MinecraftServer server) {
        var registryManager = server.getCombinedDynamicRegistries().getCombinedRegistryManager();
        DataFixer dataFixer = server.getDataFixer();
        Lifecycle registryLifecycle = registryManager.getRegistryLifecycle();

        var levelDynamic = getLevelProperties(levelData, dataFixer);
        var dynamic = wrap(levelDynamic, registryManager);

        WorldGenSettings worldGenSettings = getWorldGenSettings(dynamic);

        NbtCompound data = levelData.getCompound("Data");
        DataConfiguration dataConfiguration = getDataConfiguration(data, dataFixer);
        LevelInfo levelInfo = LevelInfo.fromDynamic(dynamic, dataConfiguration);

        // use an empty registry to only read the entries from the nbt
        Registry<DimensionOptions> existingDimOptions = new SimpleRegistry<>(RegistryKeys.DIMENSION, registryLifecycle);

        var dimensionsConfig = worldGenSettings.dimensionOptionsRegistryHolder()
                .toConfig(existingDimOptions);

        Lifecycle propsLifecycle = dimensionsConfig.getLifecycle().add(registryLifecycle);

        LevelProperties levelProperties = LevelProperties.readProperties(dynamic, levelInfo,
                dimensionsConfig.specialWorldProperty(), worldGenSettings.generatorOptions(), propsLifecycle);

        return new Result(levelProperties, dimensionsConfig);
    }

    private WorldGenSettings getWorldGenSettings(Dynamic<NbtElement> dynamic) {
        var worldGenSettingsDynamic = dynamic.get("WorldGenSettings").orElseEmptyMap();

        return WorldGenSettings.CODEC.parse(worldGenSettingsDynamic).getOrThrow();
    }

    // from net.minecraft.world.level.storage.LevelStorage.readLevelProperties(java.nio.file.Path, com.mojang.datafixers.DataFixer)
    // adjusted so that NbtCompound is used instead
    private Dynamic<NbtElement> getLevelProperties(NbtCompound nbtCompound, DataFixer dataFixer) {
        NbtCompound data = nbtCompound.getCompound("Data");
        int dataVersion = NbtHelper.getDataVersion(data, -1);

        Dynamic<NbtElement> levelProps = DataFixTypes.LEVEL.update(dataFixer, new Dynamic<>(NbtOps.INSTANCE, data), dataVersion);

        Dynamic<NbtElement> player = levelProps.get("Player").orElseEmptyMap();
        Dynamic<NbtElement> updatedPlayer = DataFixTypes.PLAYER.update(dataFixer, player, dataVersion);
        levelProps = levelProps.set("Player", updatedPlayer);

        Dynamic<NbtElement> worldGenSettings = levelProps.get("WorldGenSettings").orElseEmptyMap();
        Dynamic<NbtElement> updatedWorldGenSettings = DataFixTypes.WORLD_GEN_SETTINGS.update(dataFixer, worldGenSettings, dataVersion);
        levelProps = levelProps.set("WorldGenSettings", updatedWorldGenSettings);

        return levelProps;
    }

    private <T> Dynamic<T> wrap(Dynamic<T> dynamic, DynamicRegistryManager.Immutable registryManager) {
        RegistryOps<T> registryOps = RegistryOps.of(dynamic.getOps(), registryManager);
        return new Dynamic<>(registryOps, dynamic.getValue());
    }

    @NotNull
    private DataConfiguration getDataConfiguration(NbtCompound data, DataFixer dataFixer) {
        int dataVersion = NbtHelper.getDataVersion(data, -1);

        Dynamic<NbtElement> dataDynamic = new Dynamic<>(NbtOps.INSTANCE, data);
        Dynamic<NbtElement> dynamic = DataFixTypes.LEVEL.update(dataFixer, dataDynamic, dataVersion);

        return DataConfiguration.CODEC.parse(dynamic)
                .resultOrPartial(logger::error)
                .orElse(DataConfiguration.SAFE_MODE);
    }
}
