package work.lclpnet.kibu.world;

import net.minecraft.world.level.GameRules;

import java.util.Map;

public interface GameRuleAccess {

    Map<GameRules.Key<?>, GameRules.Value<?>> kibu$getRules();
}
