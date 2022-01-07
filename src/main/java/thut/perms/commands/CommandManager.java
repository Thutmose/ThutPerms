package thut.perms.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import thut.perms.management.PermNodes;

public class CommandManager
{

    public static boolean hasPerm(final CommandSourceStack source, final String permission)
    {
        try
        {
            final ServerPlayer player = source.getPlayerOrException();
            return CommandManager.hasPerm(player, permission);
        }
        catch (final CommandSyntaxException e)
        {
            // TODO decide what to actually do here?
            return true;
        }
    }

    public static boolean hasPerm(final ServerPlayer player, final String permission)
    {
        return PermNodes.getBooleanPerm(player, permission);
    }

}
