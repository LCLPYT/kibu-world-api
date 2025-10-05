package work.lclpnet.kibu.world.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.GameRuleCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import xyz.nucleoid.fantasy.RuntimeWorld;

@Mixin(GameRuleCommand.class)
public class GameRuleCommandMixin {

    @ModifyReceiver(
            method = { "executeQuery" },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/GameRules;get(Lnet/minecraft/world/GameRules$Key;)Lnet/minecraft/world/GameRules$Rule;"
            )
    )
    private static GameRules kibu$changeQueryReceiver(GameRules instance, GameRules.Key<?> key,
                                                 @Local(argsOnly = true) ServerCommandSource source) {
        return kibu$changeReceiver(instance, source);
    }

    @ModifyReceiver(
            method = { "executeSet" },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/GameRules;get(Lnet/minecraft/world/GameRules$Key;)Lnet/minecraft/world/GameRules$Rule;"
            )
    )
    private static GameRules kibu$changeSetReceiver(GameRules instance, GameRules.Key<?> key,
                                                      @Local(argsOnly = true) CommandContext<ServerCommandSource> ctx) {
        return kibu$changeReceiver(instance, ctx.getSource());
    }

    @Unique
    private static GameRules kibu$changeReceiver(GameRules original, ServerCommandSource source) {
        Entity entity = source.getEntity();

        if (entity == null || !(entity.getEntityWorld() instanceof RuntimeWorld rt)) {
            return original;
        }

        return rt.getGameRules();
    }
}
