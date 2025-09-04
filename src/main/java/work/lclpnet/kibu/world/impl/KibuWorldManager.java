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
import work.lclpnet.kibu.world.mixin.fantasy.RuntimeWorldPropertiesAccessor;
import xyz.nucleoid.fantasy.RuntimeWorld;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;
import xyz.nucleoid.fantasy.RuntimeWorldProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@ApiStatus.Internal
public class KibuWorldManager implements WorldManager, WorldHandleTracker, LevelDataWriter {

    private final Map<ServerWorld, RuntimeWorldHandle> worlds = new HashMap<>();
    private final Map<Identifier, RuntimeWorld> runtimeWorlds = new HashMap<>();
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
    public Optional<RuntimeWorldConfig> getRuntimeWorldConfig(Identifier identifier) {
        RuntimeWorld world;

        synchronized (this) {
            world = runtimeWorlds.get(identifier);
        }

        if (world == null) {
            return Optional.empty();
        }

        if (world.getLevelProperties() instanceof RuntimeWorldProperties rtProps) {
            return Optional.of(((RuntimeWorldPropertiesAccessor) (Object) rtProps).getConfig());
        }

        return Optional.empty();
    }

    @Override
    public void registerWorldHandle(RuntimeWorldHandle handle) {
        synchronized (this) {
            ServerWorld world = handle.asWorld();
            worlds.put(world, handle);

            if (world instanceof RuntimeWorld rt) {
                runtimeWorlds.put(world.getRegistryKey().getValue(), rt);
            }
        }
    }

    @Override
    public void unregisterWorld(ServerWorld world) {
        synchronized (this) {
            worlds.remove(world);

            if (world instanceof RuntimeWorld rt) {
                runtimeWorlds.remove(world.getRegistryKey().getValue(), rt);
            }
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
