package work.lclpnet.kibu.world;

import net.minecraft.server.world.ServerWorld;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

public interface WorldHandleTracker {

    void registerWorldHandle(RuntimeWorldHandle handle);

    void unregisterWorld(ServerWorld world);
}
