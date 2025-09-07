package work.lclpnet.kibu.world.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.kibu.world.KibuWorlds;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

@Mixin(GameRules.Rule.class)
public class GameRule$RuleMixin {

    @Inject(
            method = "changed",
            at = @At("RETURN")
    )
    private void kibu$onChanged(MinecraftServer server, CallbackInfo ci) {
        if (server == null) return;

        var handles = KibuWorlds.getInstance().getWorldManager(server).getRuntimeWorldHandles();

        // manually update tick time
        for (RuntimeWorldHandle handle : handles) {
            ServerWorld world = handle.asWorld();

            boolean tickTime = world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).get();

            ((ServerWorldAccessor) world).setShouldTickTime(tickTime);
        }
    }
}
