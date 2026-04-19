package work.lclpnet.kibu.world.data;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import work.lclpnet.kibu.world.mixin.MinecraftServerAccessor;

import java.nio.file.Path;

public interface LevelDataWriter {

    void writeLevelData(ServerLevel world, Path path);

    default void writeLevelData(ServerLevel world) {
        MinecraftServer server = world.getServer();
        LevelStorageSource.LevelStorageAccess session = ((MinecraftServerAccessor) server).getStorageSource();
        ResourceKey<Level> registryKey = world.dimension();

        Path levelDat = session.getDimensionPath(registryKey).resolve(LevelResource.LEVEL_DATA_FILE.id());

        writeLevelData(world, levelDat);
    }
}
