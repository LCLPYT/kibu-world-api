package work.lclpnet.kibu.world;

import net.minecraft.server.MinecraftServer;
import work.lclpnet.kibu.world.impl.KibuLevelsImpl;

public interface KibuLevels {

    WorldManager getWorldManager(MinecraftServer server);

    static KibuLevels getInstance() {
        return KibuLevelsImpl.getInstance();
    }
}
