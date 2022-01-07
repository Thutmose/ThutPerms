package thut.perms.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import thut.perms.Perms;
import thut.perms.management.GroupManager;
import thut.perms.management.PermNodes;
import thut.perms.management.PermNodes.DefaultPermissionLevel;

public class Reload
{

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "reload_perms";
        String perm;
        PermNodes.registerNode(perm = "command." + name, DefaultPermissionLevel.OP,
                "Can the player reload the permissions from files.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // No target argument version
        command = command.executes(ctx -> Reload.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source) throws CommandSyntaxException
    {
        Perms.config.sendFeedback(source, "thutperms.reloaded", true);
        final MinecraftServer server = source.getServer();
        Perms.loadPerms();
        GroupManager.get_instance()._server = server;

        // Refresh things for the players
        for (final ServerPlayer player : server.getPlayerList().getPlayers())
        {
            // Reload player names, to apply the tags if they exist
            GroupManager.get_instance().updateName(player);
            // Update their command lists
            server.getCommands().sendCommands(player);
        }
        return 0;
    }
}