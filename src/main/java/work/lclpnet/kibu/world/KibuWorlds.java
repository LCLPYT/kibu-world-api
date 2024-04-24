package work.lclpnet.kibu.world;

import net.minecraft.server.MinecraftServer;
import work.lclpnet.kibu.world.impl.KibuWorldsImpl;

public interface KibuWorlds {

    WorldManager getWorldManager(MinecraftServer server);

    static KibuWorlds getInstance() {
        return KibuWorldsImpl.getInstance();
    }
}
