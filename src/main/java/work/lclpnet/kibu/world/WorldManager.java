package work.lclpnet.kibu.world;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.Optional;
import java.util.Set;

public interface WorldManager {

    Set<RuntimeWorldHandle> getRuntimeWorldHandles();

    Optional<RuntimeWorldHandle> getRuntimeWorldHandle(ServerWorld world);

    /**
     * Open a persistent world created by fantasy, using stored level data.
     * @param identifier The dimension identifier.
     * @return The runtime world handle, or empty if the persistent world could not be restored.
     * @apiNote If the world could not be opened, e.g. if the returned {@link Optional} is empty,
     * the consumer should call {@link xyz.nucleoid.fantasy.Fantasy#getOrOpenPersistentWorld(Identifier, RuntimeWorldConfig)}
     * instead.
     * The reason why this method doesn't do this as fallback is because the world generator may be different.
     * Newly generated chunks in that world would then be incoherent.
     * This is why the consumer is responsible for re-creating the world.
     */
    Optional<RuntimeWorldHandle> openPersistentWorld(Identifier identifier);

    /**
     * Attempts to create a {@link RuntimeWorldConfig} from level data on disk.
     * If the level.dat file exists, this method tries to parse it and create a world config.
     * @param identifier The dimension identifier.
     * @return The runtime world config, or empty if there is no level data to load.
     */
    Optional<RuntimeWorldConfig> getWorldConfig(Identifier identifier);
}
