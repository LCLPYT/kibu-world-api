package work.lclpnet.kibu.world.init;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage;
import work.lclpnet.kibu.world.KibuWorlds;
import work.lclpnet.kibu.world.WorldHandleTracker;
import work.lclpnet.kibu.world.WorldManager;
import work.lclpnet.kibu.world.data.LevelDataWriter;
import work.lclpnet.kibu.world.mixin.MinecraftServerAccessor;
import work.lclpnet.kibu.world.mixin.fantasy.RuntimeWorldAccessor;
import xyz.nucleoid.fantasy.RuntimeWorld;

import java.nio.file.Files;
import java.nio.file.Path;

public class SafeKibuWorldsInit {

    /**
     * Called when dynamic dependencies are verified.
     */
    public void initSafe() {
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            if (!(world instanceof RuntimeWorld runtimeWorld)) return;

            WorldManager worldManager = KibuWorlds.getInstance().getWorldManager(server);

            if (worldManager instanceof WorldHandleTracker tracker) {
                tracker.unregisterWorld(runtimeWorld);
            }
        });

        ServerWorldEvents.LOAD.register((server, world) -> {
            if (!(world instanceof RuntimeWorld runtimeWorld)) return;

            RuntimeWorld.Style style = ((RuntimeWorldAccessor) runtimeWorld).getStyle();

            if (style != RuntimeWorld.Style.PERSISTENT) return;

            var key = world.getRegistryKey();

            LevelStorage.Session session = ((MinecraftServerAccessor) server).getSession();
            Path levelDat = session.getWorldDirectory(key).resolve(WorldSavePath.LEVEL_DAT.getRelativePath());

            if (Files.exists(levelDat)) return;

            WorldManager worldManager = KibuWorlds.getInstance().getWorldManager(server);

            if (worldManager instanceof LevelDataWriter writer) {
                writer.writeLevelData(world);
            }
        });
    }
}
