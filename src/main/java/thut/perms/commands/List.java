package thut.perms.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import thut.perms.Perms;
import thut.perms.management.PermNodes;
import thut.perms.management.PermNodes.DefaultPermissionLevel;
import thut.perms.management.PermissionsManager;

public class List
{

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "list_perms";
        String perm;
        PermNodes.registerNode(perm = "command." + name, DefaultPermissionLevel.OP,
                "Can the player print the list of perms to log.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name)
                .requires(cs -> CommandManager.hasPerm(cs, perm));

        // Set up the command's arguments
        command = command.executes(ctx -> List.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source)
    {
        // TODO sort this
//        Collections.sort(perms);
        try
        {
            final java.util.List<PermissionNode<?>> perms = Lists.newArrayList(PermissionAPI.getRegisteredNodes());
            final StringBuilder builder = new StringBuilder();
            builder.append("List of all known permissions:\n");
            for (final PermissionNode<?> s : perms)
            {
                String desc = s.getDescription() == null ? "No Description Provided!" : s.getDescription().getString();
                DefaultPermissionLevel level = PermissionsManager.getDefaultPermissionLevel(s);
                builder.append(s.getNodeName() + "\t" + desc + "\t" + level + "\n");
            }
            Perms.LOGGER.info(builder.toString());
            Perms.config.sendFeedback(source, "thutperms.perms.logged", true);
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return 0;
    }
}
