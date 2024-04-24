package work.lclpnet.kibu.world.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;

public interface LevelDataSerializer {

    NbtCompound serializeLevelData(ServerWorld world);
}
