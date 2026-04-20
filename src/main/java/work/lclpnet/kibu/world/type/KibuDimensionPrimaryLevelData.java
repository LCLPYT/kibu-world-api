package work.lclpnet.kibu.world.type;

import net.minecraft.world.level.storage.PrimaryLevelData;
import org.jetbrains.annotations.Nullable;

public interface KibuDimensionPrimaryLevelData {

    @Nullable
    PrimaryLevelData kibu$getPrimaryLevelData();

    void kibu$setPrimaryLevelData(PrimaryLevelData primaryLevelData);
}
