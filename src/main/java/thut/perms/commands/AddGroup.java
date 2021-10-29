package thut.perms.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.perms.Perms;
import thut.perms.management.Group;

public class AddGroup
{

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "add_group";
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP,
                "Can the player create a permissions group.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));

        // Set up the command's arguments
        command = command.then(Commands.argument("name", StringArgumentType.string()).executes(ctx -> AddGroup.execute(
                ctx.getSource(), StringArgumentType.getString(ctx, "name"))));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source, final String groupName)
    {
        Group g = Perms.getGroup(groupName);
        if (g != null)
        {
            Perms.config.sendError(source, "thutperms.group.exists");
            return 1;
        }
        try
        {
            g = Perms.addGroup(groupName);
            g.setAll(false);
            g._init = false;
            Perms.savePerms();
        }
        catch (final IllegalArgumentException e)
        {
            Perms.config.sendError(source, e.getMessage());
            return 1;
        }
        Perms.config.sendFeedback(source, "thutperms.group.created", true, groupName);
        return 0;
    }
}
