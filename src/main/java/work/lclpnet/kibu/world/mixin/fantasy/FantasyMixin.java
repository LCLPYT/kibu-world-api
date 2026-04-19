package work.lclpnet.kibu.world.mixin.fantasy;

import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.kibu.world.KibuLevels;
import work.lclpnet.kibu.world.WorldHandleTracker;
import work.lclpnet.kibu.world.WorldManager;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

@Mixin(value = Fantasy.class)
public class FantasyMixin {

    @Shadow(remap = false) @Final private MinecraftServer server;

    @Inject(
            method = "openTemporaryLevel(Lxyz/nucleoid/fantasy/RuntimeLevelConfig;)Lxyz/nucleoid/fantasy/RuntimeLevelHandle;",
            at = @At("RETURN")
    )
    public void kibu$openTemporaryLevel(RuntimeLevelConfig config, CallbackInfoReturnable<RuntimeLevelHandle> cir) {
        RuntimeLevelHandle handle = cir.getReturnValue();
        WorldManager worldManager = KibuLevels.getInstance().getWorldManager(server);

        if (worldManager instanceof WorldHandleTracker tracker) {
            tracker.registerWorldHandle(handle);
        }
    }

    @Inject(
            method = "getOrOpenPersistentLevel",
            at = @At("RETURN")
    )
    public void kibu$getOrOpenPersistentWorld(Identifier key, RuntimeLevelConfig config, CallbackInfoReturnable<RuntimeLevelHandle> cir) {
        RuntimeLevelHandle handle = cir.getReturnValue();
        WorldManager worldManager = KibuLevels.getInstance().getWorldManager(server);

        if (worldManager instanceof WorldHandleTracker tracker) {
            tracker.registerWorldHandle(handle);
        }
    }
}
