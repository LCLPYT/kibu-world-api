package work.lclpnet.kibu.world.data;

import com.mojang.serialization.Dynamic;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.jspecify.annotations.NonNull;

public interface LevelDataDeserializer {

    @NonNull PrimaryLevelData deserializePrimaryLevelData(
            WorldGenSettings worldGenSettings,
            Dynamic<?> levelDataTag,
            MinecraftServer server
    );
}
