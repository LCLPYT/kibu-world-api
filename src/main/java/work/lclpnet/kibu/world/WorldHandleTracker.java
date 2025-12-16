package work.lclpnet.kibu.world;

import net.minecraft.server.level.ServerLevel;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

public interface WorldHandleTracker {

    void registerWorldHandle(RuntimeWorldHandle handle);

    void unregisterWorld(ServerLevel world);
}
