package work.lclpnet.kibu.world.data;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.World;
import net.minecraft.world.level.storage.LevelStorage;
import work.lclpnet.kibu.world.mixin.MinecraftServerAccessor;

import java.nio.file.Path;

public interface LevelDataWriter {

    void writeLevelData(ServerWorld world, Path path);

    default void writeLevelData(ServerWorld world) {
        MinecraftServer server = world.getServer();
        LevelStorage.Session session = ((MinecraftServerAccessor) server).getSession();
        RegistryKey<World> registryKey = world.getRegistryKey();

        Path levelDat = session.getWorldDirectory(registryKey).resolve(WorldSavePath.LEVEL_DAT.getRelativePath());

        writeLevelData(world, levelDat);
    }
}
