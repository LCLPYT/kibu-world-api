package work.lclpnet.kibu.world.init;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.lclpnet.kibu.world.KibuWorlds;
import work.lclpnet.kibu.world.WorldHandleTracker;
import work.lclpnet.kibu.world.WorldManager;
import work.lclpnet.kibu.world.data.LevelDataWriter;
import work.lclpnet.kibu.world.mixin.MinecraftServerAccessor;
import work.lclpnet.kibu.world.mixin.fantasy.RuntimeWorldAccessor;
import xyz.nucleoid.fantasy.RuntimeWorld;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.nio.file.Files;
import java.nio.file.Path;

public class KibuWorldsInit implements ModInitializer {

    private static final String KIBU_WORLD_API_MOD_ID = "kibu-world-api";
    public static final Logger LOGGER = LoggerFactory.getLogger(KIBU_WORLD_API_MOD_ID);

    @Override
    public void onInitialize() {
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            if (!(world instanceof RuntimeWorld runtimeWorld)) return;

            WorldManager worldManager = KibuWorlds.getInstance().getWorldManager(server);

            if (worldManager instanceof WorldHandleTracker tracker) {
                tracker.unregisterWorld(runtimeWorld);
            }
        });

        // needs to be removed if merged into upstream (https://github.com/NucleoidMC/fantasy/pull/72)
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (!(world instanceof RuntimeWorld runtimeWorld)) return;

            // setup world border for the runtime world (method is named poorly in yarn mappings)
            runtimeWorld.getServer().getPlayerList().addWorldborderListener(runtimeWorld);
        });

        ServerWorldEvents.LOAD.register((server, world) -> {
            if (!(world instanceof RuntimeWorld runtimeWorld)) return;

            RuntimeWorld.Style style = ((RuntimeWorldAccessor) runtimeWorld).getStyle();

            if (style != RuntimeWorld.Style.PERSISTENT) return;

            var key = world.dimension();

            LevelStorageSource.LevelStorageAccess session = ((MinecraftServerAccessor) server).getStorageSource();
            Path levelDat = session.getDimensionPath(key).resolve(LevelResource.LEVEL_DATA_FILE.getId());

            if (Files.exists(levelDat)) return;

            WorldManager worldManager = KibuWorlds.getInstance().getWorldManager(server);

            if (worldManager instanceof LevelDataWriter writer) {
                writer.writeLevelData(world);
            }
        });

        ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> {
            WorldManager worldManager = KibuWorlds.getInstance().getWorldManager(server);

            if (worldManager instanceof LevelDataWriter writer) {
                for (RuntimeWorldHandle handle : worldManager.getRuntimeWorldHandles()) {
                    writer.writeLevelData(handle.asWorld());
                }
            }
        });

        LOGGER.info("Initialized.");
    }
}
