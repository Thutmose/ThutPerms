package thut.permissions.commands;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import thut.permissions.GroupManager;
import thut.permissions.ThutPerms;
import thut.permissions.util.BaseCommand;

public class Reload extends BaseCommand
{

    public Reload()
    {
        super(CommandManager.reload);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        ThutPerms.loadPerms();
        GroupManager.get_instance()._server = server;
        // Reload player names, to apply the tags if they exist
        for (EntityPlayer player : server.getPlayerList().getPlayers())
        {
            GroupManager.get_instance()._manager.createPlayer(player);
            player.refreshDisplayName();
        }
        sender.sendMessage(new TextComponentString("Reloaded Permissions from File"));
    }

}
