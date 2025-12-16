package work.lclpnet.kibu.world.mixin;

import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.storage.DerivedLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DerivedLevelData.class)
public interface DerivedLevelDataAccessor {

    @Accessor
    WorldData getWorldData();
}
