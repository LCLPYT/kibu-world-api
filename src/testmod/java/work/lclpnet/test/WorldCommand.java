package work.lclpnet.test;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import work.lclpnet.kibu.world.KibuWorlds;
import work.lclpnet.kibu.world.WorldManager;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class WorldCommand {

    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(command());
    }

    private LiteralArgumentBuilder<ServerCommandSource> command() {
        return CommandManager.literal("kibu:world")
                .requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                                .executes(this::createWorld)))
                .then(CommandManager.literal("tp")
                        .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                                .suggests(this::worldSuggestions)
                                .executes(this::tpWorld)));
    }

    private int createWorld(CommandContext<ServerCommandSource> ctx) {
        Identifier id = IdentifierArgumentType.getIdentifier(ctx, "id");

        ServerCommandSource source = ctx.getSource();
        MinecraftServer server = source.getServer();
        WorldManager worldManager = KibuWorlds.getInstance().getWorldManager(server);

        var handle = worldManager.openPersistentWorld(id);

        if (handle.isEmpty()) {
            source.sendMessage(Text.literal("Failed to open world"));
            return 0;
        }

        source.sendMessage(Text.literal("Opened world " + id));
        return 1;
    }

    private int tpWorld(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Identifier id = IdentifierArgumentType.getIdentifier(ctx, "id");

        ServerCommandSource source = ctx.getSource();
        MinecraftServer server = source.getServer();

        var key = RegistryKey.of(RegistryKeys.WORLD, id);
        ServerWorld world = server.getWorld(key);

        if (world == null) {
            source.sendMessage(Text.literal("World %s is not loaded".formatted(id)));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayerOrThrow();
        player.teleport(world, 0, 100, 0, Set.of(), 0, 0, true);

        source.sendMessage(Text.literal("Teleported to " + id));
        return 1;
    }

    private CompletableFuture<Suggestions> worldSuggestions(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        MinecraftServer server = ctx.getSource().getServer();
        if (server == null) return builder.buildFuture();

        for (var key : server.getWorldRegistryKeys()) {
            ServerWorld world = server.getWorld(key);
            if (world == null) continue;

            builder.suggest(key.getValue().toString());
        }

        return builder.buildFuture();
    }
}
