package thut.permissions.util;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.server.permission.PermissionAPI;

public class CommandWrapper implements ICommand
{
    final ICommand wrapped;
    final String   node;

    public CommandWrapper(ICommand wrapped)
    {
        this.wrapped = wrapped;
        node = "command." + wrapped.getName();
    }

    @Override
    public List<String> getAliases()
    {
        return wrapped.getAliases();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        wrapped.execute(server, sender, args);
    }

    @Override
    public int compareTo(ICommand o)
    {
        return wrapped.compareTo(o);
    }

    @Override
    public String getName()
    {
        return wrapped.getName();
    }

    @Override
    public String getUsage(ICommandSource sender)
    {
        return wrapped.getUsage(sender);
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSource sender)
    {
        if (sender instanceof ServerPlayerEntity) return PermissionAPI.hasPermission((PlayerEntity) sender, node);
        else return true;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSource sender, String[] args,
            BlockPos targetPos)
    {
        return wrapped.getTabCompletions(server, sender, args, targetPos);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        return wrapped.isUsernameIndex(args, index);
    }

}
