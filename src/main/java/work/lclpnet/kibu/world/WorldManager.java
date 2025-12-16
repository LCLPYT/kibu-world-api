package work.lclpnet.kibu.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.Optional;
import java.util.Set;

public interface WorldManager {

    Set<RuntimeWorldHandle> getRuntimeWorldHandles();

    Optional<RuntimeWorldHandle> getRuntimeWorldHandle(ServerLevel world);

    /**
     * Open a persistent world created by fantasy, using stored level data.
     * @param identifier The dimension identifier.
     * @return The runtime world handle, or empty if the persistent world could not be restored.
     * @apiNote If the world could not be opened, e.g. if the returned {@link Optional} is empty,
     * the consumer should call {@link xyz.nucleoid.fantasy.Fantasy#getOrOpenPersistentWorld(ResourceLocation, RuntimeWorldConfig)}
     * instead.
     * The reason why this method doesn't do this as fallback is because the world generator may be different.
     * Newly generated chunks in that world would then be incoherent.
     * This is why the consumer is responsible for re-creating the world.
     */
    Optional<RuntimeWorldHandle> openPersistentWorld(ResourceLocation identifier);

    /**
     * Attempts to create a {@link RuntimeWorldConfig} from level data on disk.
     * If the level.dat file exists, this method tries to parse it and create a world config.
     * @param identifier The dimension identifier.
     * @return The runtime world config, or empty if there is no level data to load.
     */
    Optional<RuntimeWorldConfig> getWorldConfig(ResourceLocation identifier);

    /**
     * Gets the {@link RuntimeWorldConfig} of a {@link xyz.nucleoid.fantasy.RuntimeWorld} with the given dimension identifier.
     * @param identifier The dimension identifier.
     * @return The runtime world config of the given {@link xyz.nucleoid.fantasy.RuntimeWorld} dimension,
     * or empty if there is no {@link xyz.nucleoid.fantasy.RuntimeWorld} with that dimension identifier.
     */
    Optional<RuntimeWorldConfig> getRuntimeWorldConfig(ResourceLocation identifier);

    /**
     * Gets the {@link RuntimeWorldConfig} of a {@link ServerLevel} if it is a {@link xyz.nucleoid.fantasy.RuntimeWorld}.
     * @param world The runtime world.
     * @return The runtime world config of the given {@link xyz.nucleoid.fantasy.RuntimeWorld} dimension,
     * or empty if there given world is not a {@link xyz.nucleoid.fantasy.RuntimeWorld}.
     */
    default Optional<RuntimeWorldConfig> getRuntimeWorldConfig(ServerLevel world) {
        return getRuntimeWorldConfig(world.dimension().location());
    }
}
