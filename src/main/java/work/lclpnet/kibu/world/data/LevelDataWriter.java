package work.lclpnet.kibu.world.data;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.jetbrains.annotations.NotNull;
import work.lclpnet.kibu.world.mixin.MinecraftServerAccessor;

import java.nio.file.Path;

public interface LevelDataWriter {

    @NotNull
    PrimaryLevelData getOrCreatePrimaryLevelData(ServerLevel level);

    void writeLevelData(ServerLevel world, Path dimensionPath);

    default void writeLevelData(ServerLevel world) {
        MinecraftServer server = world.getServer();
        LevelStorageSource.LevelStorageAccess session = ((MinecraftServerAccessor) server).getStorageSource();
        ResourceKey<Level> registryKey = world.dimension();

        Path dimensionPath = session.getDimensionPath(registryKey);

        writeLevelData(world, dimensionPath);
    }
}
