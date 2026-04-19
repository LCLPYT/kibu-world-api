package work.lclpnet.kibu.world.mixin.fantasy;

import net.minecraft.world.level.storage.PrimaryLevelData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import work.lclpnet.kibu.world.KibuLevelConfig;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;

@Mixin(RuntimeLevelConfig.class)
public class RuntimeLevelConfigMixin implements KibuLevelConfig {

    @Unique
    private PrimaryLevelData primaryLevelData = null;

    @Override
    public @Nullable PrimaryLevelData kibu$getPrimaryLevelData() {
        return primaryLevelData;
    }

    @Override
    public void kibu$setPrimaryLevelData(PrimaryLevelData primaryLevelData) {
        this.primaryLevelData = primaryLevelData;
    }
}
