package work.lclpnet.kibu.world.mixin.fantasy;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldProperties;

@Mixin(value = RuntimeWorldProperties.class, remap = false)
public interface RuntimeWorldPropertiesAccessor {

    @Accessor(remap = false)
    RuntimeWorldConfig getConfig();
}
