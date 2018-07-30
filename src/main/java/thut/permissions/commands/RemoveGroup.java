package thut.permissions.commands;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
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
    public String getUsage(ICommandSender sender)
    {
        return super.getUsage(sender) + " <name>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        String groupName = args[0];
        Group g = ThutPerms.getGroup(groupName);
        if (g == null) throw new CommandException("Error, specified Group does not exist.");
        if (g == GroupManager.instance.initial || g == GroupManager.instance.mods)
            throw new CommandException("Error, cannot remove default groups.");
        GroupManager.instance.groups.remove(g);
        GroupManager.instance._groupNameMap.remove(groupName);
        ThutPerms.savePerms();
        sender.sendMessage(new TextComponentString("Removed group " + groupName));
    }

}
