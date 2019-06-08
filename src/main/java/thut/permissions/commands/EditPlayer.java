package thut.permissions.commands;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.text.StringTextComponent;
import thut.permissions.Group;
import thut.permissions.GroupManager;
import thut.permissions.Player;
import thut.permissions.PlayerManager;
import thut.permissions.ThutPerms;
import thut.permissions.util.BaseCommand;

public class EditPlayer extends BaseCommand
{

    public EditPlayer()
    {
        super(CommandManager.editPlayer);
    }

    @Override
    public String getUsage(ICommandSource sender)
    {
        return super.getUsage(sender) + " <playername> <permission> <value>";
    }

    /** Return whether the specified command parameter index is a username
     * parameter. */
    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        return index == 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        String playerName = args[0];
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
        id = profile.getId();
        String permission = args[1];
        boolean all = permission.equalsIgnoreCase("all");
        boolean reset = permission.equalsIgnoreCase("reset");
        boolean add = permission.equalsIgnoreCase("!add");
        boolean remove = permission.equalsIgnoreCase("!remove");
        boolean check = permission.equalsIgnoreCase("!groups");
        PlayerManager manager = GroupManager.get_instance()._manager;
        Player player = manager.getPlayer(id);

        if (add)
        {
            if (args.length < 3) throw new CommandException(super.getUsage(sender) + " !add <group>");
            String groupName = args[2];
            Group g = ThutPerms.getGroup(groupName);
            if (g == null) { throw new CommandException("Error, Specifed group does not exist."); }
            player.addGroup(g);
            manager.savePlayer(id);
            return;
        }

        if (remove)
        {
            if (args.length < 3) throw new CommandException(super.getUsage(sender) + " !remove <group>");
            String groupName = args[2];
            Group g = ThutPerms.getGroup(groupName);
            if (g == null) { throw new CommandException("Error, Specifed group does not exist."); }
            player.removeGroup(g);
            manager.savePlayer(id);
            return;
        }

        if (check)
        {
            sender.sendMessage(new StringTextComponent("Personal Groups for " + player + ":"));
            for (String s : player.groups)
            {
                sender.sendMessage(new StringTextComponent(s));
            }
        }

        if (reset)
        {
            player.setAll(false);
            player.getAllowedCommands().clear();
            player.getBannedCommands().clear();
            sender.sendMessage(new StringTextComponent("Removed personal settings for " + playerName));
            manager.savePlayer(id);
            return;
        }

        check = args.length == 2;
        if (check)
        {
            if (all)
            {
                sender.sendMessage(
                        new StringTextComponent("All permission state for " + playerName + " is " + player.isAll()));
                return;
            }
            sender.sendMessage(new StringTextComponent(
                    "Permission for " + playerName + " is " + player.hasPermission(permission)));
            return;
        }
        boolean value = Boolean.parseBoolean(args[2]);
        if (all)
        {
            player.setAll(value);
            sender.sendMessage(
                    new StringTextComponent("All permission state for " + playerName + " set to " + player.isAll()));
        }
        else
        {
            if (value)
            {
                if (!player.getAllowedCommands().contains(permission)) player.getAllowedCommands().add(permission);
                player.getBannedCommands().remove(permission);
            }
            else
            {
                player.getAllowedCommands().remove(permission);
                if (!player.getBannedCommands().contains(permission)) player.getBannedCommands().add(permission);
            }
            sender.sendMessage(new StringTextComponent(
                    "Permission for " + playerName + " set to " + player.hasPermission(permission)));
        }
        player._init = false;
        manager.savePlayer(id);
    }

}
