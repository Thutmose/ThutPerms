package thut.permissions.commands;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import thut.permissions.Group;
import thut.permissions.GroupManager;
import thut.permissions.ThutPerms;
import thut.permissions.util.BaseCommand;

public class RemoveGroup extends BaseCommand
{

    public RemoveGroup()
    {
        super(CommandManager.removeGroup);
    }

    @Override
    public String getUsage(ICommandSource sender)
    {
        return super.getUsage(sender) + " <name>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        String groupName = args[0];
        Group g = ThutPerms.getGroup(groupName);
        if (g == null) throw new CommandException("Error, specified Group does not exist.");
        if (g == GroupManager.get_instance().initial || g == GroupManager.get_instance().mods)
            throw new CommandException("Error, cannot remove default groups.");
        GroupManager.get_instance().groups.remove(g);
        GroupManager.get_instance()._groupNameMap.remove(groupName);
        ThutPerms.savePerms();
        sender.sendMessage(new StringTextComponent("Removed group " + groupName));
    }

}
