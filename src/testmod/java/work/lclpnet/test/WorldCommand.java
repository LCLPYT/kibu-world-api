package work.lclpnet.test;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import work.lclpnet.kibu.world.KibuWorlds;
import work.lclpnet.kibu.world.WorldManager;
import work.lclpnet.kibu.world.init.KibuWorldsInit;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class WorldCommand {

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(command());
    }

    private LiteralArgumentBuilder<CommandSourceStack> command() {
        return literal("kibu:world")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(literal("create")
                        .then(literal("temporary")
                                .executes(this::createTmp))
                        .then(literal("permanent")
                                .then(argument("id", IdentifierArgument.id())
                                        .executes(this::createPermanent))))
                .then(literal("load")
                        .then(Commands.argument("id", IdentifierArgument.id())
                                .executes(this::loadWorld)))
                .then(literal("tp")
                        .then(Commands.argument("id", IdentifierArgument.id())
                                .suggests(this::worldSuggestions)
                                .executes(this::tpWorld)));
    }

    private int createPermanent(CommandContext<CommandSourceStack> ctx) {
        Identifier id = IdentifierArgument.getId(ctx, "id");

        MinecraftServer server = ctx.getSource().getServer();

        RuntimeWorldConfig config = new RuntimeWorldConfig()
                .setDimensionType(BuiltinDimensionTypes.OVERWORLD)
                .setDifficulty(Difficulty.NORMAL)
                .setGenerator(server.overworld().getChunkSource().getGenerator())
                .setSeed(123);

        RuntimeWorldHandle handle;

        try {
            handle = Fantasy.get(server).getOrOpenPersistentWorld(id, config);
        } catch (Throwable t) {
            KibuWorldsInit.LOGGER.error("Failed to open persistent world", t);
            ctx.getSource().sendFailure(Component.literal("Failed to open permanent world. More details in the console"));
            return 0;
        }

        ctx.getSource().sendSystemMessage(Component.literal("Opened permanent world " + handle.getRegistryKey().identifier()));

        return 1;
    }

    private int createTmp(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();

        RuntimeWorldConfig config = new RuntimeWorldConfig()
                .setDimensionType(BuiltinDimensionTypes.OVERWORLD)
                .setDifficulty(Difficulty.NORMAL)
                .setGenerator(server.overworld().getChunkSource().getGenerator())
                .setSeed(123);

        RuntimeWorldHandle handle;

        try {
            handle = Fantasy.get(server).openTemporaryWorld(config);
        } catch (Throwable t) {
            KibuWorldsInit.LOGGER.error("Failed to create temporary world", t);
            ctx.getSource().sendFailure(Component.literal("Failed to create temporary world. More details in the console"));
            return 0;
        }

        ctx.getSource().sendSystemMessage(Component.literal("Created temporary world " + handle.getRegistryKey().identifier()));

        return 1;
    }

    private int loadWorld(CommandContext<CommandSourceStack> ctx) {
        Identifier id = IdentifierArgument.getId(ctx, "id");

        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        WorldManager worldManager = KibuWorlds.getInstance().getWorldManager(server);

        var handle = worldManager.openPersistentWorld(id);

        if (handle.isEmpty()) {
            source.sendSystemMessage(Component.literal("Failed to open world"));
            return 0;
        }

        source.sendSystemMessage(Component.literal("Opened world " + id));
        return 1;
    }

    private int tpWorld(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Identifier id = IdentifierArgument.getId(ctx, "id");

        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();

        var key = ResourceKey.create(Registries.DIMENSION, id);
        ServerLevel world = server.getLevel(key);

        if (world == null) {
            source.sendSystemMessage(Component.literal("World %s is not loaded".formatted(id)));
            return 0;
        }

        ServerPlayer player = source.getPlayerOrException();
        player.teleportTo(world, 0, 100, 0, Set.of(), 0, 0, true);

        source.sendSystemMessage(Component.literal("Teleported to " + id));
        return 1;
    }

    private CompletableFuture<Suggestions> worldSuggestions(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        MinecraftServer server = ctx.getSource().getServer();

        for (var key : server.levelKeys()) {
            ServerLevel world = server.getLevel(key);
            if (world == null) continue;

            builder.suggest(key.identifier().toString());
        }

        return builder.buildFuture();
    }
}
