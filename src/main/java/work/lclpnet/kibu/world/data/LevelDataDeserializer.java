package work.lclpnet.kibu.world.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.storage.PrimaryLevelData;

public interface LevelDataDeserializer {

    Result deserializeLevelData(CompoundTag levelData, MinecraftServer server);

    record Result(PrimaryLevelData properties, WorldDimensions.Complete dimensions) {}
}
