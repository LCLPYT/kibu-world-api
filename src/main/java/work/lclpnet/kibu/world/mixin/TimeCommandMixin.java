package work.lclpnet.kibu.world.mixin;

import com.google.common.collect.Iterables;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TimeCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TimeCommand.class)
public class TimeCommandMixin {

    @WrapOperation(
            method = {"executeSet", "executeAdd"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;getWorlds()Ljava/lang/Iterable;"
            )
    )
    private static Iterable<ServerWorld> kibu$filterWorlds(MinecraftServer instance, Operation<Iterable<ServerWorld>> original,
                                                          @Local(argsOnly = true) ServerCommandSource source) {

        Iterable<ServerWorld> worlds = original.call(instance);
        ServerPlayerEntity player = source.getPlayer();

        if (player == null) {
            return worlds;
        }

        return Iterables.filter(worlds, player.getEntityWorld()::equals);
    }
}
