package work.lclpnet.kibu.world.mixin;

import com.google.common.collect.Iterables;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.TimeCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TimeCommand.class)
public class TimeCommandMixin {

    @WrapOperation(
            method = {"setTime", "addTime"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;getAllLevels()Ljava/lang/Iterable;"
            )
    )
    private static Iterable<ServerLevel> kibu$filterWorlds(MinecraftServer instance, Operation<Iterable<ServerLevel>> original,
                                                           @Local(argsOnly = true) CommandSourceStack source) {

        Iterable<ServerLevel> worlds = original.call(instance);
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            return worlds;
        }

        return Iterables.filter(worlds, player.level()::equals);
    }
}
