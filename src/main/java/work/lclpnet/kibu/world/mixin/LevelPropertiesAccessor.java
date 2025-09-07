package work.lclpnet.kibu.world.mixin;

import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelProperties.class)
public interface LevelPropertiesAccessor {

    @Mutable
    @Accessor
    void setGeneratorOptions(GeneratorOptions generatorOptions);
}
