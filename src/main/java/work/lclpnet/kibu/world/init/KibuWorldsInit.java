package work.lclpnet.kibu.world.init;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.lclpnet.kibu.world.KibuLevels;
import work.lclpnet.kibu.world.WorldHandleTracker;
import work.lclpnet.kibu.world.WorldManager;
import work.lclpnet.kibu.world.data.LevelDataWriter;
import work.lclpnet.kibu.world.mixin.MinecraftServerAccessor;
import work.lclpnet.kibu.world.mixin.fantasy.RuntimeLevelAccessor;
import xyz.nucleoid.fantasy.RuntimeLevel;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

import java.nio.file.Files;
import java.nio.file.Path;

public class KibuWorldsInit implements ModInitializer {

    private static final String KIBU_WORLD_API_MOD_ID = "kibu-world-api";
    public static final Logger LOGGER = LoggerFactory.getLogger(KIBU_WORLD_API_MOD_ID);

    @Override
    public void onInitialize() {
        ServerLevelEvents.UNLOAD.register((server, world) -> {
            if (!(world instanceof RuntimeLevel RuntimeLevel)) return;

            WorldManager worldManager = KibuLevels.getInstance().getWorldManager(server);

            if (worldManager instanceof WorldHandleTracker tracker) {
                tracker.unregisterWorld(RuntimeLevel);
            }
        });

        // needs to be removed if merged into upstream (https://github.com/NucleoidMC/fantasy/pull/72)
        ServerLevelEvents.LOAD.register((_, world) -> {
            if (!(world instanceof RuntimeLevel runtimeLevel)) return;

            // setup world border for the runtime world (method is named poorly in yarn mappings)
            runtimeLevel.getServer().getPlayerList().addWorldborderListener(runtimeLevel);
        });

        ServerLevelEvents.LOAD.register((server, world) -> {
            if (!(world instanceof RuntimeLevel runtimeLevel)) return;

            RuntimeLevel.Style style = ((RuntimeLevelAccessor) runtimeLevel).getStyle();

            if (style != RuntimeLevel.Style.PERSISTENT) return;

            var key = world.dimension();

            LevelStorageSource.LevelStorageAccess session = ((MinecraftServerAccessor) server).getStorageSource();
            Path levelDat = session.getDimensionPath(key).resolve(LevelResource.LEVEL_DATA_FILE.id());

            if (Files.exists(levelDat)) return;

            WorldManager worldManager = KibuLevels.getInstance().getWorldManager(server);

            if (worldManager instanceof LevelDataWriter writer) {
                writer.writeLevelData(world);
            }
        });

        ServerLifecycleEvents.AFTER_SAVE.register((server, _, _) -> {
            WorldManager worldManager = KibuLevels.getInstance().getWorldManager(server);

            if (worldManager instanceof LevelDataWriter writer) {
                for (RuntimeLevelHandle handle : worldManager.getRuntimeLevelHandles()) {
                    writer.writeLevelData(handle.asLevel());
                }
            }
        });

        LOGGER.info("Initialized.");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(KIBU_WORLD_API_MOD_ID, path);
    }
}
