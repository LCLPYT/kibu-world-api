package work.lclpnet.kibu.world.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.kibu.world.KibuLevels;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(
            method = "onGameRuleChanged",
            at = @At("RETURN")
    )
    public void kibu$onGameRuleChanged(GameRule<?> rule, Object value, CallbackInfo ci) {
        var self = (MinecraftServer) (Object) this;
        var handles = KibuLevels.getInstance().getWorldManager(self).getRuntimeLevelHandles();

        // manually update tick time
        for (RuntimeLevelHandle handle : handles) {
            ServerLevel world = handle.asLevel();

            boolean tickTime = world.getGameRules().get(GameRules.ADVANCE_TIME);

            ((ServerLevelAccessor) world).setTickTime(tickTime);
        }
    }
}
