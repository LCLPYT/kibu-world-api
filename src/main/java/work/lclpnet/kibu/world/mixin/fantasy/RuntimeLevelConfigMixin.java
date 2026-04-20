package work.lclpnet.kibu.world.mixin.fantasy;

import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import work.lclpnet.kibu.world.type.KibuDimensionWeatherData;
import work.lclpnet.kibu.world.type.KibuDimensionPrimaryLevelData;
import work.lclpnet.kibu.world.type.KibuLevelConfig;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;

@Mixin(RuntimeLevelConfig.class)
public class RuntimeLevelConfigMixin implements KibuDimensionPrimaryLevelData, KibuDimensionWeatherData, KibuLevelConfig {

    @Unique
    private PrimaryLevelData primaryLevelData = null;
    @Unique
    private WeatherData dimensionWeatherData = null;
    @Unique
    private boolean mirrorOverworldWeatherData = false;

    @Override
    public @Nullable PrimaryLevelData kibu$getPrimaryLevelData() {
        return primaryLevelData;
    }

    @Override
    public void kibu$setPrimaryLevelData(PrimaryLevelData primaryLevelData) {
        this.primaryLevelData = primaryLevelData;
    }

    @Override
    public @Nullable WeatherData kibu$getDimensionWeatherData() {
        return dimensionWeatherData;
    }

    @Override
    public void kibu$setDimensionWeatherData(WeatherData dimensionWeatherData) {
        this.dimensionWeatherData = dimensionWeatherData;
    }

    @Override
    public boolean kibu$mirrorOverworldWeatherData() {
        return mirrorOverworldWeatherData;
    }

    @Override
    public void kibu$setMirrorOverworldWeatherData(boolean mirror) {
        this.mirrorOverworldWeatherData = mirror;
    }
}
