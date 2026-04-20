package work.lclpnet.kibu.world.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.WeatherCommand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.WeatherData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.nucleoid.fantasy.RuntimeLevel;

@Mixin(WeatherCommand.class)
public class WeatherCommandMixin {

    @WrapOperation(
            method = {"setRain", "setClear", "setThunder"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;setWeatherParameters(IIZZ)V"
            )
    )
    private static void kibu$changeReceiver(
            MinecraftServer instance,
            int clearTime,
            int rainTime,
            boolean raining,
            boolean thundering,
            Operation<Void> original,
            @Local(argsOnly = true, name = "source") CommandSourceStack source
    ) {
        Entity entity = source.getEntity();

        if (entity != null && entity.level() instanceof RuntimeLevel level && level.canHaveWeather()) {
            // from net.minecraft.server.MinecraftServer.setWeatherParameters
            WeatherData weatherData = level.getWeatherData();
            weatherData.setClearWeatherTime(clearTime);
            weatherData.setRainTime(rainTime);
            weatherData.setThunderTime(rainTime);
            weatherData.setRaining(raining);
            weatherData.setThundering(thundering);
            return;
        }

        original.call(instance, clearTime, rainTime, raining, thundering);
    }
}
