package thut.permissions.commands;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import thut.permissions.Group;
import thut.permissions.GroupManager;
import thut.permissions.ThutPerms;
import thut.permissions.util.BaseCommand;

public class GroupInfo extends BaseCommand
{

    public GroupInfo()
    {
        super(CommandManager.groupInfo);
    }

    @Override
    public String getUsage(ICommandSource sender)
    {
        return super.getUsage(sender) + "<player|exists|hasPerms|members|groups|listCommands|perms> <arguments>";
    }

    /** Return whether the specified command parameter index is a username
     * parameter. */
    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        if (args[0].equalsIgnoreCase("player")) return index == 1;
        return false;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        if (args.length == 0) throw new CommandException(getUsage(sender));
        if (args[0].equalsIgnoreCase("player"))
        {
            String playerName = args[1];
            UUID id = null;
            GameProfile profile = null;
            try
            {
                PlayerEntity test = getPlayer(server, sender, playerName);
                id = test.getUniqueID();
                profile = test.getGameProfile();
            }
            catch (Exception e1)
            {
                try
                {
                    id = UUID.fromString(playerName);
                }
                catch (Exception e)
                {
                }
                profile = new GameProfile(id, playerName);
                profile = TileEntitySkull.updateGameprofile(profile);
            }
            if (profile.getId() == null) { throw new CommandException("Error, cannot find profile for " + playerName); }
            Group current = GroupManager.get_instance().getPlayerGroup(profile.getId());
            if (current == null) sender.sendMessage(new StringTextComponent(playerName + " is not in a group"));
            else sender.sendMessage(new StringTextComponent(playerName + " is currently in " + current.name));
            return;
        }
        else if (args[0].equalsIgnoreCase("exists"))
        {
            String groupName = args[1];
            Group g = ThutPerms.getGroup(groupName);
            if (g != null) sender.sendMessage(new StringTextComponent("Group " + groupName + " exists."));
            else sender.sendMessage(new StringTextComponent("Group " + groupName + "does not exist."));
            return;
        }
        else if (args[0].equalsIgnoreCase("hasPerms"))
        {
            String groupName = args[1];
            String perm = args[2];
            Group g = ThutPerms.getGroup(groupName);
            if (g == null) { throw new CommandException("Error, specified Group does not exist."); }
            if (g.hasPermission(perm))
                sender.sendMessage(new StringTextComponent("Group " + groupName + " can use " + perm));
            else sender.sendMessage(new StringTextComponent("Group " + groupName + " can not use " + perm));
            return;
        }
        else if (args[0].equalsIgnoreCase("members"))
        {
            String groupName = args[1];
            Group g = ThutPerms.getGroup(groupName);
            if (g == null) { throw new CommandException("Error, specified Group does not exist."); }
            sender.sendMessage(new StringTextComponent("Members of Group " + groupName));
            for (UUID id : g.members)
            {
                GameProfile profile = getProfile(server, id);
                sender.sendMessage(new StringTextComponent(profile.getName()));
            }
            return;
        }
        else if (args[0].equalsIgnoreCase("groups"))
        {
            sender.sendMessage(new StringTextComponent("List of existing Groups:"));
            sender.sendMessage(new StringTextComponent(GroupManager.get_instance().initial.name));
            sender.sendMessage(new StringTextComponent(GroupManager.get_instance().mods.name));
            for (Group g : GroupManager.get_instance().groups)
            {
                sender.sendMessage(new StringTextComponent(g.name));
            }
            return;
        }
        else if (args[0].equalsIgnoreCase("ListCommands"))
        {
            sender.sendMessage(new StringTextComponent("List of existing commands:"));
            for (ICommand command : FMLCommonHandler.instance().getMinecraftServerInstance().getCommandManager()
                    .getCommands().values())
            {
                String name = command.getName();
                sender.sendMessage(new StringTextComponent(name + "->" + command.getClass().getName()));
            }
            return;
        }
        else if (args[0].equalsIgnoreCase("perms"))
        {
            String groupName = args[1];
            Group g = ThutPerms.getGroup(groupName);
            if (g == null) { throw new CommandException("Error, specified Group does not exist."); }
            sender.sendMessage(new StringTextComponent("List of allowed commands:"));
            for (String s : g.getAllowedCommands())
            {
                sender.sendMessage(new StringTextComponent(s));
            }
            sender.sendMessage(new StringTextComponent("all set to: " + g.isAll()));
            return;
        }
        throw new CommandException(getUsage(sender));
    }

}
