package work.lclpnet.kibu.world.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelData;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.kibu.world.init.KibuWorldsInit;

import java.util.Optional;

public class KibuLevelData extends SavedData {

    public static final Codec<KibuLevelData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("flat_world").forGetter(KibuLevelData::isFlatWorld),
            Difficulty.CODEC.fieldOf("difficulty").forGetter(KibuLevelData::getDifficulty),
            LevelData.RespawnData.CODEC.optionalFieldOf("spawn").forGetter(KibuLevelData::getRespawnData),
            Codec.LONG.fieldOf("time").forGetter(KibuLevelData::getGameTime)
    ).apply(instance, (flatWorld, difficulty, respawnData, gameTime) ->
            new KibuLevelData(flatWorld, difficulty, respawnData.orElse(null), gameTime)));

    public static final SavedDataType<KibuLevelData> TYPE = new SavedDataType<>(
            KibuWorldsInit.id("level_data"),
            () -> new KibuLevelData(false, Difficulty.NORMAL, null, 0L),
            CODEC,
            null  // TODO ok?
    );

    private final boolean flatWorld;
    private final Difficulty difficulty;
    private final @Nullable LevelData.RespawnData respawnData;
    private final long gameTime;

    public KibuLevelData(boolean flatWorld, Difficulty difficulty, @Nullable LevelData.RespawnData respawnData, long gameTime) {
        this.flatWorld = flatWorld;
        this.difficulty = difficulty;
        this.respawnData = respawnData;
        this.gameTime = gameTime;
    }

    public boolean isFlatWorld() {
        return flatWorld;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public Optional<LevelData.RespawnData> getRespawnData() {
        return Optional.ofNullable(respawnData);
    }

    public long getGameTime() {
        return gameTime;
    }
}
