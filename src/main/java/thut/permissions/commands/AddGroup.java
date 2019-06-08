package thut.permissions.commands;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import thut.permissions.Group;
import thut.permissions.ThutPerms;
import thut.permissions.util.BaseCommand;

public class AddGroup extends BaseCommand
{
    public AddGroup()
    {
        super(CommandManager.addGroup);
    }

    @Override
    public String getUsage(ICommandSource sender)
    {
        return super.getUsage(sender) + " <groupname>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        String groupName = args[0];
        Group g = ThutPerms.getGroup(groupName);
        if (g != null) { throw new CommandException("Error, Group already exists, cannot create again."); }
        g = ThutPerms.addGroup(groupName);
        g.setAll(false);
        g._init = false;
        for (String node : ThutPerms.manager.getRegisteredNodes())
        {
            if (ThutPerms.manager.getDefaultPermissionLevel(node) == DefaultPermissionLevel.ALL)
            {
                g.getAllowedCommands().add(node);
            }
        }
        ThutPerms.savePerms();
        sender.sendMessage(new StringTextComponent("Created group " + groupName));
    }

}
