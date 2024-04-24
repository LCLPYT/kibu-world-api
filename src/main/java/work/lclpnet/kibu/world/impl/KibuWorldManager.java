package work.lclpnet.kibu.world.impl;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import work.lclpnet.kibu.world.WorldHandleTracker;
import work.lclpnet.kibu.world.WorldManager;
import work.lclpnet.kibu.world.data.LevelDataWriter;
import work.lclpnet.kibu.world.init.KibuWorldsInit;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@ApiStatus.Internal
public class KibuWorldManager implements WorldManager, WorldHandleTracker, LevelDataWriter {

    private final Map<ServerWorld, RuntimeWorldHandle> worlds = new HashMap<>();
    private final LevelDataService levelDataService;
    private final WorldPersistenceService worldPersistenceService;

    public KibuWorldManager(MinecraftServer server) {
        this.levelDataService = new LevelDataService(KibuWorldsInit.LOGGER);
        this.worldPersistenceService = new WorldPersistenceService(server, levelDataService, KibuWorldsInit.LOGGER);
    }

    public Set<RuntimeWorldHandle> getRuntimeWorldHandles() {
        synchronized (this) {
            return new HashSet<>(worlds.values());
        }
    }

    @Override
    public Optional<RuntimeWorldHandle> getRuntimeWorldHandle(ServerWorld world) {
        RuntimeWorldHandle handle;

        synchronized (this) {
            handle = worlds.get(world);
        }

        return Optional.ofNullable(handle);
    }

    @Override
    public Optional<RuntimeWorldHandle> openPersistentWorld(Identifier identifier) {
        return worldPersistenceService.tryRecreateWorld(identifier);
    }

    @Override
    public Optional<RuntimeWorldConfig> getWorldConfig(Identifier identifier) {
        var registryKey = RegistryKey.of(RegistryKeys.WORLD, identifier);

        RuntimeWorldConfig config = worldPersistenceService.restoreConfig(registryKey);

        return Optional.ofNullable(config);
    }

    @Override
    public void registerWorldHandle(RuntimeWorldHandle handle) {
        synchronized (this) {
            worlds.put(handle.asWorld(), handle);
        }
    }

    @Override
    public void unregisterWorld(ServerWorld world) {
        synchronized (this) {
            worlds.remove(world);
        }
    }

    @Override
    public void writeLevelData(ServerWorld world, Path path) {
        try {
            NbtCompound nbt = levelDataService.serializeLevelData(world);

            try (var out = Files.newOutputStream(path)) {
                NbtIo.writeCompressed(nbt, out);
            }
        } catch (IOException e) {
            KibuWorldsInit.LOGGER.error("Failed to write level data to {}", path, e);
        }
    }
}
