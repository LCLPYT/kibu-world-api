package work.lclpnet.kibu.world;

import net.minecraft.server.level.ServerLevel;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

public interface WorldHandleTracker {

    void registerWorldHandle(RuntimeLevelHandle handle);

    void unregisterWorld(ServerLevel world);
}
