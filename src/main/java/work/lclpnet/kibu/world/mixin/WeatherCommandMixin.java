package work.lclpnet.kibu.world.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.WeatherCommand;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WeatherCommand.class)
public class WeatherCommandMixin {

    @ModifyReceiver(
            method = { "executeRain", "executeClear", "executeThunder" },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerWorld;setWeather(IIZZ)V"
            )
    )
    private static ServerWorld kibu$changeReceiver(ServerWorld instance, int clearDuration, int rainDuration, boolean raining, boolean thundering,
                                                   @Local(argsOnly = true) ServerCommandSource source) {

        Entity entity = source.getEntity();

        if (entity == null) return instance;

        // set weather of the source entity dimension, if it has weather
        if (entity.getWorld() instanceof ServerWorld world && world.getDimension().hasSkyLight()) {
            return world;
        }

        return instance;
    }
}
