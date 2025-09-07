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
import net.minecraft.world.Difficulty;
import net.minecraft.world.dimension.DimensionTypes;
import work.lclpnet.kibu.world.KibuWorlds;
import work.lclpnet.kibu.world.WorldManager;
import work.lclpnet.kibu.world.init.KibuWorldsInit;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class WorldCommand {

    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(command());
    }

    private LiteralArgumentBuilder<ServerCommandSource> command() {
        return literal("kibu:world")
                .requires(s -> s.hasPermissionLevel(2))
                .then(literal("create")
                        .then(literal("temporary")
                                .executes(this::createTmp))
                        .then(literal("permanent")
                                .then(argument("id", IdentifierArgumentType.identifier())
                                        .executes(this::createPermanent))))
                .then(literal("load")
                        .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                                .executes(this::loadWorld)))
                .then(literal("tp")
                        .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                                .suggests(this::worldSuggestions)
                                .executes(this::tpWorld)));
    }

    private int createPermanent(CommandContext<ServerCommandSource> ctx) {
        Identifier id = IdentifierArgumentType.getIdentifier(ctx, "id");

        MinecraftServer server = ctx.getSource().getServer();

        RuntimeWorldConfig config = new RuntimeWorldConfig()
                .setDimensionType(DimensionTypes.OVERWORLD)
                .setDifficulty(Difficulty.NORMAL)
                .setGenerator(server.getOverworld().getChunkManager().getChunkGenerator())
                .setSeed(123);

        RuntimeWorldHandle handle;

        try {
            handle = Fantasy.get(server).getOrOpenPersistentWorld(id, config);
        } catch (Throwable t) {
            KibuWorldsInit.LOGGER.error("Failed to open persistent world", t);
            ctx.getSource().sendError(Text.literal("Failed to open permanent world. More details in the console"));
            return 0;
        }

        ctx.getSource().sendMessage(Text.literal("Opened permanent world " + handle.getRegistryKey().getValue()));

        return 1;
    }

    private int createTmp(CommandContext<ServerCommandSource> ctx) {
        MinecraftServer server = ctx.getSource().getServer();

        RuntimeWorldConfig config = new RuntimeWorldConfig()
                .setDimensionType(DimensionTypes.OVERWORLD)
                .setDifficulty(Difficulty.NORMAL)
                .setGenerator(server.getOverworld().getChunkManager().getChunkGenerator())
                .setSeed(123);

        RuntimeWorldHandle handle;

        try {
            handle = Fantasy.get(server).openTemporaryWorld(config);
        } catch (Throwable t) {
            KibuWorldsInit.LOGGER.error("Failed to create temporary world", t);
            ctx.getSource().sendError(Text.literal("Failed to create temporary world. More details in the console"));
            return 0;
        }

        ctx.getSource().sendMessage(Text.literal("Created temporary world " + handle.getRegistryKey().getValue()));

        return 1;
    }

    private int loadWorld(CommandContext<ServerCommandSource> ctx) {
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
