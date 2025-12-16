package work.lclpnet.kibu.world.impl;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
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

    private final Map<ServerLevel, RuntimeWorldHandle> worlds = new HashMap<>();
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
    public Optional<RuntimeWorldHandle> getRuntimeWorldHandle(ServerLevel world) {
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
        var registryKey = ResourceKey.create(Registries.DIMENSION, identifier);

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

        if (world.getLevelData() instanceof RuntimeWorldProperties rtProps) {
            return Optional.of(((RuntimeWorldPropertiesAccessor) (Object) rtProps).getConfig());
        }

        return Optional.empty();
    }

    @Override
    public void registerWorldHandle(RuntimeWorldHandle handle) {
        synchronized (this) {
            ServerLevel world = handle.asWorld();
            worlds.put(world, handle);

            if (world instanceof RuntimeWorld rt) {
                runtimeWorlds.put(world.dimension().identifier(), rt);
            }
        }
    }

    @Override
    public void unregisterWorld(ServerLevel world) {
        synchronized (this) {
            worlds.remove(world);

            if (world instanceof RuntimeWorld rt) {
                runtimeWorlds.remove(world.dimension().identifier(), rt);
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
