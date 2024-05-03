package work.lclpnet.kibu.world.mixin.fantasy;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.kibu.world.KibuWorlds;
import work.lclpnet.kibu.world.WorldHandleTracker;
import work.lclpnet.kibu.world.WorldManager;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

@Mixin(value = Fantasy.class, remap = false)
public class FantasyMixin {

    @Shadow @Final private MinecraftServer server;

    @Inject(
            method = "openTemporaryWorld(Lnet/minecraft/util/Identifier;Lxyz/nucleoid/fantasy/RuntimeWorldConfig;)Lxyz/nucleoid/fantasy/RuntimeWorldHandle;",
            at = @At("RETURN"),
            remap = false
    )
    public void kibu$openTemporaryWorld(Identifier key, RuntimeWorldConfig config, CallbackInfoReturnable<RuntimeWorldHandle> cir) {
        RuntimeWorldHandle handle = cir.getReturnValue();
        WorldManager worldManager = KibuWorlds.getInstance().getWorldManager(server);

        if (worldManager instanceof WorldHandleTracker tracker) {
            tracker.registerWorldHandle(handle);
        }
    }

    @Inject(
            method = "getOrOpenPersistentWorld",
            at = @At("RETURN"),
            remap = false
    )
    public void kibu$getOrOpenPersistentWorld(Identifier key, RuntimeWorldConfig config, CallbackInfoReturnable<RuntimeWorldHandle> cir) {
        RuntimeWorldHandle handle = cir.getReturnValue();
        WorldManager worldManager = KibuWorlds.getInstance().getWorldManager(server);

        if (worldManager instanceof WorldHandleTracker tracker) {
            tracker.registerWorldHandle(handle);
        }
    }
}
