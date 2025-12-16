package work.lclpnet.kibu.world.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.commands.GameRuleCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import xyz.nucleoid.fantasy.RuntimeWorld;

@Mixin(GameRuleCommand.class)
public class GameRuleCommandMixin {

    @ModifyReceiver(
            method = {"queryRule"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/GameRules;getRule(Lnet/minecraft/world/level/GameRules$Key;)Lnet/minecraft/world/level/GameRules$Value;"
            )
    )
    private static GameRules kibu$changeQueryReceiver(GameRules instance, GameRules.Key<?> key,
                                                      @Local(argsOnly = true) CommandSourceStack source) {
        return kibu$changeReceiver(instance, source);
    }

    @ModifyReceiver(
            method = {"setRule"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/GameRules;getRule(Lnet/minecraft/world/level/GameRules$Key;)Lnet/minecraft/world/level/GameRules$Value;"
            )
    )
    private static GameRules kibu$changeSetReceiver(GameRules instance, GameRules.Key<?> key,
                                                    @Local(argsOnly = true) CommandContext<CommandSourceStack> ctx) {
        return kibu$changeReceiver(instance, ctx.getSource());
    }

    @Unique
    private static GameRules kibu$changeReceiver(GameRules original, CommandSourceStack source) {
        Entity entity = source.getEntity();

        if (entity == null || !(entity.level() instanceof RuntimeWorld rt)) {
            return original;
        }

        return rt.getGameRules();
    }
}
