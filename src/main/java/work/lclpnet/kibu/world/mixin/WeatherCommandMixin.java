package work.lclpnet.kibu.world.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.WeatherCommand;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WeatherCommand.class)
public class WeatherCommandMixin {

    // TODO
//    @ModifyReceiver(
//            method = {"setRain", "setClear", "setThunder"},
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lnet/minecraft/server/level/ServerLevel;setWeatherParameters(IIZZ)V"
//            )
//    )
//    private static ServerLevel kibu$changeReceiver(ServerLevel instance, int clearDuration, int rainDuration, boolean raining, boolean thundering,
//                                                   @Local(argsOnly = true) CommandSourceStack source) {
//
//        Entity entity = source.getEntity();
//
//        if (entity == null) return instance;
//
//        // set weather of the source entity dimension, if it has weather
//        if (entity.level() instanceof ServerLevel world && world.dimensionType().hasSkyLight()) {
//            return world;
//        }
//
//        return instance;
//    }
}
