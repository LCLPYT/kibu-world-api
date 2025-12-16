package work.lclpnet.kibu.world.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

public interface LevelDataSerializer {

    CompoundTag serializeLevelData(ServerLevel world);
}
