package work.lclpnet.kibu.world.impl;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.ApiStatus;
import work.lclpnet.kibu.world.WorldHandleTracker;
import work.lclpnet.kibu.world.WorldManager;
import work.lclpnet.kibu.world.data.LevelDataWriter;
import work.lclpnet.kibu.world.init.KibuWorldsInit;
import xyz.nucleoid.fantasy.RuntimeLevel;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@ApiStatus.Internal
public class KibuWorldManager implements WorldManager, WorldHandleTracker, LevelDataWriter {

    private final Map<ServerLevel, RuntimeLevelHandle> worlds = new HashMap<>();
    private final Map<Identifier, RuntimeLevel> runtimeLevels = new HashMap<>();
    private final LevelDataService levelDataService;
    private final WorldPersistenceService worldPersistenceService;

    public KibuWorldManager(MinecraftServer server) {
        this.levelDataService = new LevelDataService(KibuWorldsInit.LOGGER);
        this.worldPersistenceService = new WorldPersistenceService(server, levelDataService, KibuWorldsInit.LOGGER);
    }

    public Set<RuntimeLevelHandle> getRuntimeLevelHandles() {
        synchronized (this) {
            return new HashSet<>(worlds.values());
        }
    }

    @Override
    public Optional<RuntimeLevelHandle> getRuntimeLevelHandle(ServerLevel level) {
        RuntimeLevelHandle handle;

        synchronized (this) {
            handle = worlds.get(level);
        }

        return Optional.ofNullable(handle);
    }

    @Override
    public Optional<RuntimeLevelHandle> openPersistentWorld(Identifier identifier) {
        return worldPersistenceService.tryRecreateWorld(identifier);
    }

    @Override
    public Optional<RuntimeLevelConfig> getWorldConfig(Identifier identifier) {
        var registryKey = ResourceKey.create(Registries.DIMENSION, identifier);

        RuntimeLevelConfig config = worldPersistenceService.restoreConfig(registryKey);

        return Optional.ofNullable(config);
    }

    @Override
    public Optional<RuntimeLevelConfig> getRuntimeLevelConfig(Identifier identifier) {
        RuntimeLevel world;

        synchronized (this) {
            world = runtimeLevels.get(identifier);
        }

        if (world == null) {
            return Optional.empty();
        }

        // TODO
//        if (world.getLevelData() instanceof RuntimeLevelProperties rtProps) {
//            return Optional.of(((RuntimeLevelPropertiesAccessor) (Object) rtProps).getConfig());
//        }

        return Optional.empty();
    }

    @Override
    public void registerWorldHandle(RuntimeLevelHandle handle) {
        synchronized (this) {
            ServerLevel world = handle.asLevel();
            worlds.put(world, handle);

            if (world instanceof RuntimeLevel rt) {
                runtimeLevels.put(world.dimension().identifier(), rt);
            }
        }
    }

    @Override
    public void unregisterWorld(ServerLevel world) {
        synchronized (this) {
            worlds.remove(world);

            if (world instanceof RuntimeLevel rt) {
                runtimeLevels.remove(world.dimension().identifier(), rt);
            }
        }
    }

    @Override
    public void writeLevelData(ServerLevel world, Path path) {
        try {
            CompoundTag nbt = levelDataService.serializeLevelData(world);

            try (var out = Files.newOutputStream(path)) {
                NbtIo.writeCompressed(nbt, out);
            }
        } catch (IOException e) {
            KibuWorldsInit.LOGGER.error("Failed to write level data to {}", path, e);
        }
    }
}
