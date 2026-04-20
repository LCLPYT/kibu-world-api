package work.lclpnet.kibu.world.mixin.fantasy;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.kibu.world.type.KibuDimensionPrimaryLevelData;
import work.lclpnet.kibu.world.type.KibuDimensionWeatherData;
import work.lclpnet.kibu.world.type.KibuLevelConfig;
import xyz.nucleoid.fantasy.RuntimeLevel;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;

@Mixin(RuntimeLevel.class)
public class RuntimeLevelMixin implements KibuDimensionWeatherData, KibuDimensionPrimaryLevelData {

    @Unique
    private WeatherData dimensionWeatherData = null;
    @Unique
    private PrimaryLevelData primaryLevelData = null;

    @Override
    public @Nullable WeatherData kibu$getDimensionWeatherData() {
        return dimensionWeatherData;
    }

    @Override
    public void kibu$setDimensionWeatherData(WeatherData dimensionWeatherData) {
        this.dimensionWeatherData = dimensionWeatherData;
    }

    @Override
    public @Nullable PrimaryLevelData kibu$getPrimaryLevelData() {
        return primaryLevelData;
    }

    @Override
    public void kibu$setPrimaryLevelData(PrimaryLevelData primaryLevelData) {
        this.primaryLevelData = primaryLevelData;
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
        if (!((KibuLevelConfig) (Object) config).kibu$mirrorOverworldWeatherData()) {
            @SuppressWarnings("DataFlowIssue")
            @Nullable
            WeatherData weatherData = ((KibuDimensionWeatherData) (Object) config).kibu$getDimensionWeatherData();

            if (weatherData == null) {
                weatherData = new WeatherData();
            }

            this.kibu$setDimensionWeatherData(weatherData);
        }

        @SuppressWarnings("DataFlowIssue")
        @Nullable
        PrimaryLevelData levelData = ((KibuDimensionPrimaryLevelData) (Object) config).kibu$getPrimaryLevelData();

        if (levelData != null) {
            this.kibu$setPrimaryLevelData(levelData);
        }
    }
}
