package thut.permissions.commands;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
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
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        ThutPerms.loadPerms();
        GroupManager.get_instance()._server = server;
        // Reload player names, to apply the tags if they exist
        for (PlayerEntity player : server.getPlayerList().getPlayers())
        {
            GroupManager.get_instance()._manager.createPlayer(player);
            player.refreshDisplayName();
        }
        sender.sendMessage(new StringTextComponent("Reloaded Permissions from File"));
    }

}
