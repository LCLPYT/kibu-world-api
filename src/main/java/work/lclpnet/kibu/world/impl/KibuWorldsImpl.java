package work.lclpnet.kibu.world.impl;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.ApiStatus;
import work.lclpnet.kibu.world.KibuWorlds;
import work.lclpnet.kibu.world.WorldManager;

import java.util.WeakHashMap;

@ApiStatus.Internal
public final class KibuWorldsImpl implements KibuWorlds {

    private final WeakHashMap<MinecraftServer, KibuWorldManager> servers = new WeakHashMap<>();

    private KibuWorldsImpl() {}

    @Override
    public WorldManager getWorldManager(MinecraftServer server) {
        return servers.computeIfAbsent(server, KibuWorldManager::new);
    }

    public static KibuWorldsImpl getInstance() {
        return Holder.INSTANCE;
    }

    // lazy, thread-safe singleton
    private static class Holder {
        private static final KibuWorldsImpl INSTANCE = new KibuWorldsImpl();
    }
}
