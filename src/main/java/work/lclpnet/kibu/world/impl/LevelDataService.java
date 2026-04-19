package work.lclpnet.kibu.world.impl;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import work.lclpnet.kibu.world.data.LevelDataSerializer;
import work.lclpnet.kibu.world.mixin.PrimaryLevelDataAccessor;
import xyz.nucleoid.fantasy.RuntimeLevel;

import java.util.Optional;
import java.util.stream.Stream;

@ApiStatus.Internal
public class LevelDataService implements LevelDataSerializer {

    private final Logger logger;

    public LevelDataService(Logger logger) {
        this.logger = logger;
    }

    @Override
    public CompoundTag serializeLevelData(ServerLevel level) {
        // TODO
//        PrimaryLevelData saveProperties = getLevelProperties(level);
//
//        RegistryAccess registryManager = getCustomRegistryManager(level);
//
//        CompoundTag data = saveProperties.createTag(null);

        CompoundTag nbt = new CompoundTag();
//        nbt.put("Data", data);

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
        if (world instanceof RuntimeLevel RuntimeLevel) {
            PrimaryLevelDataAccessor writer = (PrimaryLevelDataAccessor) props;

            // copy generator options here
            WorldOptions genOpts = new WorldOptions(
                    RuntimeLevel.getSeed(),
                    RuntimeLevel.structureManager().shouldGenerateStructures(),
                    false
            );

            // TODO
//            writer.setWorldOptions(genOpts);
        }

        // vanilla encapsulation
        props.setDifficulty(worldProps.getDifficulty());
        props.setSpawn(worldProps.getRespawnData());
        props.setGameTime(worldProps.getGameTime());
//        props.setDayTime(worldProps.getDayTime());
        props.setDifficultyLocked(worldProps.isDifficultyLocked());
//        props.setRaining(worldProps.isRaining());
//        props.setThundering(worldProps.isThundering());

        if (worldProps instanceof ServerLevelData swProps) {
//            props.getGameRules().setAll(swProps.getGameRules(), null);
//            props.setClearWeatherTime(swProps.getClearWeatherTime());
//            props.setRainTime(swProps.getRainTime());
//            props.setThunderTime(swProps.getThunderTime());
            props.setGameType(swProps.getGameType());
            props.setInitialized(swProps.isInitialized());
//            props.setWanderingTraderId(swProps.getWanderingTraderId());
//            props.setWanderingTraderSpawnChance(swProps.getWanderingTraderSpawnChance());
//            props.setWanderingTraderSpawnDelay(swProps.getWanderingTraderSpawnDelay());
//            props.setLegacyWorldBorderSettings(swProps.getLegacyWorldBorderSettings());
        }

        return props;
    }

    /**
     * Clones given {@link WorldData} and narrows the type to {@link PrimaryLevelData}.
     * @param properties The save properties to clone.
     * @return Cloned properties as level properties.
     */
    private PrimaryLevelData cloneSaveProperties(MinecraftServer server, WorldData properties) {
        throw new RuntimeException("Not implemented");
//        CompoundTag data = properties.createTag(null);
//
//        CompoundTag nbt = new CompoundTag();
//        nbt.put("Data", data);
//
//        return deserializeLevelData(nbt, server).properties();
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
            public <E> @NonNull Optional<Registry<E>> lookup(@NonNull ResourceKey<? extends Registry<? extends E>> key) {
                if (Registries.LEVEL_STEM.equals(key)) {
                    return Optional.of((Registry<E>) registry);
                }

                return parent.lookup(key);
            }

            @Override
            public @NonNull Stream<RegistryEntry<?>> registries() {
                return Stream.concat(
                        Stream.of(new RegistryEntry<>(Registries.LEVEL_STEM, registry)),
                        parent.registries().filter(entry -> !Registries.LEVEL_STEM.equals(entry.key()))
                );
            }
        };
    }
}
