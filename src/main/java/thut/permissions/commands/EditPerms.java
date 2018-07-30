package thut.permissions.commands;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.google.common.collect.Lists;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.config.Configuration;
import thut.permissions.Group;
import thut.permissions.ThutPerms;
import thut.permissions.util.BaseCommand;

public class EditPerms extends BaseCommand
{

    public EditPerms()
    {
        super(CommandManager.editPerms);
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return super.getUsage(sender) + " <group> <perm> <value> or " + super.getUsage(sender)
                + " allowUse <optional|value> or " + super.getUsage(sender) + " list";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length == 3)
        {
            String groupName = args[0];
            String command = args[1];
            boolean enable = Boolean.parseBoolean(args[2]);
            Group g = ThutPerms.getGroup(groupName);
            if (g == null) { throw new CommandException("Error, Group not found, please create it first."); }
            if (command.equalsIgnoreCase("all"))
            {
                g.all = enable;
                sender.sendMessage(new TextComponentString("Set all Permission for " + groupName + " to " + enable));
                ThutPerms.savePerms();
                return;
            }
            g._init = false;
            if (enable)
            {
                g.allowedCommands.add(command);
                g.bannedCommands.remove(command);
            }
            else
            {
                g.allowedCommands.remove(command);
                g.bannedCommands.add(command);
            }
            sender.sendMessage(new TextComponentString("Set Permission for " + groupName + " " + enable));
            ThutPerms.savePerms();
            return;
        }
        else if (args[0].equals("allowUse"))
        {
            if (args.length == 2)
            {
                boolean enable = Boolean.parseBoolean(args[1]);
                ThutPerms.allCommandUse = enable;
                Configuration config = new Configuration(ThutPerms.configFile);
                config.load();
                config.get(Configuration.CATEGORY_GENERAL, "allCommandUse", enable).set(enable);
                config.save();
                sender.sendMessage(new TextComponentString(
                        "Set players able to use all commands allowed for their group to " + enable));
                ThutPerms.savePerms();
                ThutPerms.setAnyCommandUse(server, enable);
                return;
            }
            sender.sendMessage(new TextComponentString(
                    "Players allowed to use all commands for group: " + ThutPerms.allCommandUse));
            return;
        }
        else if (args[0].equals("list"))
        {
            List<String> perms = Lists.newArrayList(ThutPerms.manager.getRegisteredNodes());
            Collections.sort(perms);
            StringBuilder builder = new StringBuilder();
            builder.append("List of all known permissions:\n");
            for (String s : perms)
                builder.append(s + "\t" + ThutPerms.manager.getNodeDescription(s) + "\t"
                        + ThutPerms.manager.getDefaultPermissionLevel(s) + "\n");
            ThutPerms.logger.log(Level.INFO, builder.toString());
            sender.sendMessage(new TextComponentString("Logged all registered permissions nodes."));
        }
        else if (args[0].equals("toggledebug"))
        {
            ThutPerms.debug = !ThutPerms.debug;
            sender.sendMessage(new TextComponentString("Debug Mode set to " + ThutPerms.debug));
        }
        else throw new CommandException(getUsage(sender));
    }

}
