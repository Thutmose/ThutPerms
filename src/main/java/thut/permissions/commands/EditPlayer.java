package thut.permissions.commands;

import com.mojang.authlib.GameProfile;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.text.TextComponentString;
import thut.permissions.GroupManager;
import thut.permissions.Player;
import thut.permissions.util.BaseCommand;

public class EditPlayer extends BaseCommand
{

    public EditPlayer()
    {
        super(CommandManager.editPlayer);
    }

    @Override
    public String getUsage(ICommandSender sender)
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
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        String playerName = args[0];
        GameProfile profile = new GameProfile(null, playerName);
        profile = TileEntitySkull.updateGameprofile(profile);
        if (profile.getId() == null) { throw new CommandException("Error, cannot find profile for " + playerName); }
        String permission = args[1];
        boolean all = permission.equalsIgnoreCase("all");
        boolean reset = permission.equalsIgnoreCase("reset");

        if (reset)
        {
            Player player = GroupManager.instance.playerIDMap.remove(profile.getId());
            if (player != null) GroupManager.instance.players.remove(profile.getId());
            sender.sendMessage(new TextComponentString("Removed personal settings for " + playerName));
            return;
        }

        boolean check = args.length == 2;
        if (check)
        {
            Player player = GroupManager.instance.playerIDMap.get(profile.getId());
            if (player == null) throw new CommandException("No custom permissions for " + playerName);
            if (all)
            {
                sender.sendMessage(
                        new TextComponentString("All permission state for " + playerName + " is " + player.all));
                return;
            }
            sender.sendMessage(new TextComponentString(
                    "Permission for " + playerName + " is " + player.allowedCommands.contains(permission)));
            return;
        }
        boolean value = Boolean.getBoolean(args[2]);
        Player player = GroupManager.instance.playerIDMap.get(profile.getId());
        if (player == null) player = GroupManager.instance.createPlayer(profile.getId());
        if (all)
        {
            player.all = value;
            sender.sendMessage(
                    new TextComponentString("All permission state for " + playerName + " set to " + player.all));
        }
        else
        {
            player.allowedCommands.add(permission);
            sender.sendMessage(new TextComponentString(
                    "Permission for " + playerName + " set to " + player.allowedCommands.contains(permission)));
        }
    }

}
