package work.lclpnet.kibu.world.mixin.fantasy;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.fantasy.RuntimeLevelData;

@Mixin(value = RuntimeLevelData.class, remap = false)
public interface RuntimeLevelDataAccessor {

    @Accessor(remap = false)
    RuntimeLevelConfig getConfig();
}
