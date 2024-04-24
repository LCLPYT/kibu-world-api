package work.lclpnet.kibu.world;

import net.minecraft.world.GameRules;

import java.util.Map;

public interface GameRuleAccess {

    Map<GameRules.Key<?>, GameRules.Rule<?>> kibu$getRules();
}
