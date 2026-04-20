package work.lclpnet.kibu.world.type;

import net.minecraft.world.level.saveddata.WeatherData;
import org.jetbrains.annotations.Nullable;

public interface KibuDimensionWeatherData {

    @Nullable
    WeatherData kibu$getDimensionWeatherData();

    void kibu$setDimensionWeatherData(WeatherData dimensionWeatherData);
}
