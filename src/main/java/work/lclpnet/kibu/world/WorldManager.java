package work.lclpnet.kibu.world;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

import java.util.Optional;
import java.util.Set;

public interface WorldManager {

    Set<RuntimeLevelHandle> getRuntimeLevelHandles();

    Optional<RuntimeLevelHandle> getRuntimeLevelHandle(ServerLevel level);

    /**
     * Open a persistent level created by fantasy, using stored level data.
     * @param identifier The dimension identifier.
     * @return The runtime level handle, or empty if the persistent world could not be restored.
     * @apiNote If the world could not be opened, e.g. if the returned {@link Optional} is empty,
     * the consumer should call {@link xyz.nucleoid.fantasy.Fantasy#getOrOpenPersistentLevel(Identifier, RuntimeLevelConfig)}
     * instead.
     * The reason why this method doesn't do this as fallback is because the world generator may be different.
     * Newly generated chunks in that world would then be incoherent.
     * This is why the consumer is responsible for re-creating the world.
     */
    Optional<RuntimeLevelHandle> openPersistentLevel(Identifier identifier);

    /**
     * Attempts to create a {@link } from level data on disk.
     * If the level.dat file exists, this method tries to parse it and create a world config.
     * @param identifier The dimension identifier.
     * @return The runtime world config, or empty if there is no level data to load.
     */
    Optional<RuntimeLevelConfig> getStoredLevelConfig(Identifier identifier);

    /**
     * Gets the {@link } of a {@link xyz.nucleoid.fantasy.RuntimeLevel} with the given dimension identifier.
     * @param identifier The dimension identifier.
     * @return The runtime world config of the given {@link xyz.nucleoid.fantasy.RuntimeLevel} dimension,
     * or empty if there is no {@link xyz.nucleoid.fantasy.RuntimeLevel} with that dimension identifier.
     */
    Optional<RuntimeLevelConfig> getRuntimeLevelConfig(Identifier identifier);

    /**
     * Gets the {@link RuntimeLevelConfig} of a {@link ServerLevel} if it is a {@link xyz.nucleoid.fantasy.RuntimeLevel}.
     * @param level The runtime level.
     * @return The runtime level config of the given {@link xyz.nucleoid.fantasy.RuntimeLevel} dimension,
     * or empty if there given level is not a {@link xyz.nucleoid.fantasy.RuntimeLevel}.
     */
    default Optional<RuntimeLevelConfig> getRuntimeLevelConfig(ServerLevel level) {
        return getRuntimeLevelConfig(level.dimension().identifier());
    }
}
