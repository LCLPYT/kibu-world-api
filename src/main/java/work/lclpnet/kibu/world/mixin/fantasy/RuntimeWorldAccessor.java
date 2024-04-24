package work.lclpnet.kibu.world.mixin.fantasy;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xyz.nucleoid.fantasy.RuntimeWorld;

@Mixin(value = RuntimeWorld.class, remap = false)
public interface RuntimeWorldAccessor {

    @Accessor
    RuntimeWorld.Style getStyle();
}
