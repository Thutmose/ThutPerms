package thut.perms.commands;

import java.util.Collections;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.perms.Perms;

public class List
{

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "list_perms";
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP,
                "Can the player print the list of perms to log.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));

        // Set up the command's arguments
        command = command.executes(ctx -> List.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source)
    {
        final java.util.List<String> perms = Lists.newArrayList(Perms.manager.getRegisteredNodes());
        Collections.sort(perms);
        final StringBuilder builder = new StringBuilder();
        builder.append("List of all known permissions:\n");
        for (final String s : perms)
            builder.append(s + "\t" + Perms.manager.getNodeDescription(s) + "\t" + Perms.manager
                    .getDefaultPermissionLevel(s) + "\n");
        Perms.LOGGER.info(builder.toString());
        Perms.config.sendFeedback(source, "thutperms.perms.logged", true);
        return 0;
    }
}
