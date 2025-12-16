package work.lclpnet.kibu.world.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.commands.GameRuleCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;
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
                    target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"
            )
    )
    private static GameRules kibu$changeQueryReceiver(GameRules instance, GameRule<?> gameRule,
                                                      @Local(argsOnly = true) CommandSourceStack source) {
        return kibu$changeReceiver(instance, source);
    }

    @ModifyReceiver(
            method = {"setRule"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/gamerules/GameRules;set(Lnet/minecraft/world/level/gamerules/GameRule;Ljava/lang/Object;Lnet/minecraft/server/MinecraftServer;)V"
            )
    )
    private static GameRules kibu$changeSetReceiver(GameRules instance, GameRule<?> gameRule, Object object, @Nullable MinecraftServer minecraftServer,
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
