package work.lclpnet.kibu.world.mixin;

import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.UnmodifiableLevelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(UnmodifiableLevelProperties.class)
public interface UnmodifiableLevelPropertiesAccessor {

    @Accessor
    SaveProperties getSaveProperties();
}
