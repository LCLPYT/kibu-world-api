package work.lclpnet.kibu.world.mixin.fantasy;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.WeatherData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.kibu.world.type.KibuDimensionWeatherData;
import work.lclpnet.kibu.world.type.KibuLevelConfig;
import xyz.nucleoid.fantasy.RuntimeLevel;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;

@Mixin(RuntimeLevel.class)
public class RuntimeLevelMixin implements KibuDimensionWeatherData {

    @Unique
    private WeatherData dimensionWeatherData = null;

    @Override
    public @Nullable WeatherData kibu$getDimensionWeatherData() {
        return dimensionWeatherData;
    }

    @Override
    public void kibu$setDimensionWeatherData(WeatherData dimensionWeatherData) {
        this.dimensionWeatherData = dimensionWeatherData;
    }

    @Inject(
            method = "<init>(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/resources/ResourceKey;Lxyz/nucleoid/fantasy/RuntimeLevelConfig;Lxyz/nucleoid/fantasy/RuntimeLevel$Style;)V",
            at = @At("TAIL")
    )
    public void kibu$injectCustomData(
            MinecraftServer server,
            ResourceKey<Level> dimension,
            RuntimeLevelConfig config,
            RuntimeLevel.Style style,
            CallbackInfo ci
    ) {
        if (((KibuLevelConfig) (Object) config).kibu$mirrorOverworldWeatherData()) return;

        @SuppressWarnings("DataFlowIssue")
        @Nullable
        WeatherData weatherData = ((KibuDimensionWeatherData) (Object) config).kibu$getDimensionWeatherData();

        if (weatherData == null) {
            // always use independent weather data when not mirroring overworld weather
            weatherData = new WeatherData();
        }

        this.kibu$setDimensionWeatherData(weatherData);
    }
}
