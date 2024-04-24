package work.lclpnet.kibu.world.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import net.minecraft.world.level.LevelProperties;

public interface LevelDataDeserializer {

    Result deserializeLevelData(NbtCompound levelData, MinecraftServer server);

    record Result(LevelProperties properties, DimensionOptionsRegistryHolder.DimensionsConfig dimensions) {}
}
