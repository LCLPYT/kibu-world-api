package work.lclpnet.kibu.world.mixin;

import net.minecraft.world.clock.PackedClockStates;
import net.minecraft.world.clock.ServerClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerClockManager.class)
public interface ServerClockManagerAccessor {

    @Accessor
    PackedClockStates getPackedClockStates();
}
