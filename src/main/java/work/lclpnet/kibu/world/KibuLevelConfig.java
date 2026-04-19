package work.lclpnet.kibu.world;

import net.minecraft.world.level.storage.PrimaryLevelData;
import org.jetbrains.annotations.Nullable;

public interface KibuLevelConfig {

    @Nullable
    PrimaryLevelData kibu$getPrimaryLevelData();

    void kibu$setPrimaryLevelData(PrimaryLevelData primaryLevelData);
}
