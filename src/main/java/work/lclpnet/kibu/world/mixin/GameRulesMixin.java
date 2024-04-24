package work.lclpnet.kibu.world.mixin;

import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import work.lclpnet.kibu.world.GameRuleAccess;

import java.util.Collections;
import java.util.Map;

@Mixin(GameRules.class)
public class GameRulesMixin implements GameRuleAccess {

    @Shadow @Final private Map<GameRules.Key<?>, GameRules.Rule<?>> rules;

    @Override
    public Map<GameRules.Key<?>, GameRules.Rule<?>> kibu$getRules() {
        return Collections.unmodifiableMap(rules);
    }
}
