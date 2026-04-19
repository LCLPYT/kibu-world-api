package work.lclpnet.kibu.world.impl;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.ApiStatus;
import work.lclpnet.kibu.world.KibuLevels;
import work.lclpnet.kibu.world.WorldManager;

import java.util.WeakHashMap;

@ApiStatus.Internal
public final class KibuLevelsImpl implements KibuLevels {

    private final WeakHashMap<MinecraftServer, KibuWorldManager> servers = new WeakHashMap<>();

    private KibuLevelsImpl() {}

    @Override
    public WorldManager getWorldManager(MinecraftServer server) {
        return servers.computeIfAbsent(server, KibuWorldManager::new);
    }

    public static KibuLevelsImpl getInstance() {
        return Holder.INSTANCE;
    }

    // lazy, thread-safe singleton
    private static class Holder {
        private static final KibuLevelsImpl INSTANCE = new KibuLevelsImpl();
    }
}
