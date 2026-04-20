package work.lclpnet.kibu.world.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.kibu.world.type.KibuDimensionPrimaryLevelData;
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

        @Nullable
        WeatherData dimensionWeatherData = ((KibuDimensionWeatherData) level).kibu$getDimensionWeatherData();

        if (dimensionWeatherData != null) {
            cir.setReturnValue(dimensionWeatherData);
        }
    }

    @Inject(
            method = "getRespawnData",
            at = @At("HEAD"),
            cancellable = true
    )
    public void kibu$getRuntimeLevelRespawnData(CallbackInfoReturnable<LevelData.RespawnData> cir) {
        // default behavior: return server respawn data
        ServerLevel self = (ServerLevel) (Object) this;

        if (!(self instanceof RuntimeLevel level)) return;

        @Nullable
        PrimaryLevelData primaryLevelData = ((KibuDimensionPrimaryLevelData) level).kibu$getPrimaryLevelData();

        if (primaryLevelData == null) return;

        LevelData.RespawnData respawnData = primaryLevelData.getRespawnData();

        if (respawnData != null) {
            cir.setReturnValue(respawnData);
        }
    }

    @Inject(
            method = "setRespawnData",
            at = @At("HEAD"),
            cancellable = true
    )
    public void kibu$setRuntimeLevelRespawnData(LevelData.RespawnData respawnData, CallbackInfo ci) {
        // default behavior: set server respawn data and move server spawn to the specified dimension
        ServerLevel self = (ServerLevel) (Object) this;

        if (!(self instanceof RuntimeLevel level)) return;

        @Nullable
        PrimaryLevelData primaryLevelData = ((KibuDimensionPrimaryLevelData) level).kibu$getPrimaryLevelData();

        if (primaryLevelData != null && respawnData.dimension().equals(level.dimension())) {
            // vanilla discrepancy: save the spawn as local world spawn, but don't set it globally
            primaryLevelData.setSpawn(respawnData);
            ci.cancel();
        }
    }
}
