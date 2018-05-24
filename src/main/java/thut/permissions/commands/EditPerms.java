package thut.permissions.commands;

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
        return super.getUsage(sender) + " <group> <perm> <value> or /editPerms allowUse <optional|value>";
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
            g.init = false;
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
    }

}
