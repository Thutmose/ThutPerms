package thut.permissions.commands;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import thut.permissions.Group;
import thut.permissions.ThutPerms;
import thut.permissions.util.BaseCommand;

public class CopyGroup extends BaseCommand
{

    public CopyGroup()
    {
        super(CommandManager.copyGroup);
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return super.getUsage(sender) + " <from> <to>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        String groupFrom = args[0];
        Group gFrom = ThutPerms.getGroup(groupFrom);
        if (gFrom == null) { throw new CommandException("Error, specified Group " + groupFrom + " does not exist."); }
        String groupTo = args[1];
        boolean replace = true;
        if (args.length == 3 && args[2].equals("add")) replace = false;
        Group gTo = ThutPerms.getGroup(groupTo);
        if (gTo == null) { throw new CommandException("Error, specified Group " + groupTo + " does not exist."); }
        if (replace)
        {
            gTo.allowedCommands.clear();
            gTo.bannedCommands.clear();
        }

        gTo.all = gFrom.all;
        gTo.init = false;
        gTo.allowedCommands.addAll(gFrom.allowedCommands);
        gTo.bannedCommands.addAll(gFrom.bannedCommands);
        ThutPerms.savePerms();
        sender.sendMessage(new TextComponentString("Copied from " + groupFrom + " to " + groupTo));
    }

}
