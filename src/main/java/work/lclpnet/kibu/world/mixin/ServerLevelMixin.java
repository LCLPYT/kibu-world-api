package work.lclpnet.kibu.world.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.WeatherData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.kibu.world.type.KibuDimensionWeatherData;
import xyz.nucleoid.fantasy.RuntimeLevel;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Inject(
            method = "getWeatherData",
            at = @At("HEAD"),
            cancellable = true
    )
    public void kibu$perDimensionWeatherData(CallbackInfoReturnable<WeatherData> cir) {
        // default behavior: return server weather data
        ServerLevel self = (ServerLevel) (Object) this;

        if (!(self instanceof RuntimeLevel level)) return;

        WeatherData dimensionWeatherData = ((KibuDimensionWeatherData) level).kibu$getDimensionWeatherData();

        if (dimensionWeatherData != null) {
            cir.setReturnValue(dimensionWeatherData);
        }
    }
}
