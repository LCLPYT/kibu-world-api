package work.lclpnet.kibu.world.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.timer.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(LevelProperties.class)
public interface LevelPropertiesAccessor {

    @Accessor
    void setLevelInfo(LevelInfo info);

    @Accessor
    LevelInfo getLevelInfo();

    @Mutable
    @Accessor
    void setGeneratorOptions(GeneratorOptions generatorOptions);

    @Mutable
    @Accessor
    @SuppressWarnings("deprecation")
    void setSpecialProperty(LevelProperties.SpecialProperty specialProperty);

    @Accessor
    @SuppressWarnings("deprecation")
    LevelProperties.SpecialProperty getSpecialProperty();

    @Mutable
    @Accessor
    void setScheduledEvents(Timer<MinecraftServer> timer);

    @Mutable
    @Accessor
    void setRemovedFeatures(Set<String> removedFeatures);
}
