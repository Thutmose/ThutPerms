package thut.permissions.commands;

import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import thut.permissions.Group;
import thut.permissions.GroupManager;
import thut.permissions.ThutPerms;
import thut.permissions.util.BaseCommand;

public class RenameGroup extends BaseCommand
{
    public RenameGroup()
    {
        super(CommandManager.renameGroup);
    }

    @Override
    public String getUsage(ICommandSource sender)
    {
        return super.getUsage(sender) + " <oldname> <newname>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        String groupName = args[0];
        String newName = args[1];
        Group g = ThutPerms.getGroup(groupName);
        if (g == null) { throw new CommandException("Error, specified Group does not exist."); }
        Group g1 = ThutPerms.getGroup(newName);
        if (g1 != null) { throw new CommandException("Error, specified Group already exists."); }
        GroupManager.get_instance().groups.remove(g);
        GroupManager.get_instance()._groupNameMap.remove(groupName);
        g1 = ThutPerms.addGroup(newName);
        if (g == GroupManager.get_instance().initial)
        {
            GroupManager.get_instance().initial = g1;
            GroupManager.get_instance().groups.remove(g1);
        }
        else if (g == GroupManager.get_instance().mods)
        {
            GroupManager.get_instance().mods = g1;
            GroupManager.get_instance().groups.remove(g1);
        }
        g1.getAllowedCommands().addAll(g.getAllowedCommands());
        g1.getBannedCommands().addAll(g.getBannedCommands());
        Set<UUID> members = Sets.newHashSet(g.members);
        for (UUID id : members)
        {
            ThutPerms.addToGroup(id, newName);
        }
        ThutPerms.savePerms();
        sender.sendMessage(new StringTextComponent("Renamed group " + groupName + " to " + newName));
    }
}
