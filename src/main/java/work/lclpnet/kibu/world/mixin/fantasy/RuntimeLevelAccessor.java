package work.lclpnet.kibu.world.mixin.fantasy;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xyz.nucleoid.fantasy.RuntimeLevel;

@Mixin(value = RuntimeLevel.class, remap = false)
public interface RuntimeLevelAccessor {

    @Accessor
    RuntimeLevel.Style getStyle();
}
