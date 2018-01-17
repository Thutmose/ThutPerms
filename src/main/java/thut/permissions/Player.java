package thut.permissions;

import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;

import net.minecraft.command.ICommand;

public class Player
{
    public UUID        id;
    public boolean     all             = false;
    public Set<String> allowedCommands = Sets.newHashSet();

    public Player()
    {
    }

    public boolean hasPermission(String permission)
    {
        if (all) return true;
        if (permission.endsWith("*"))
        {
            permission = permission.substring(0, permission.length() - 1);
            if (permission.isEmpty()) return true;
            for (String s : allowedCommands)
            {
                if (s.startsWith(permission)) return true;
            }
            return false;
        }
        return allowedCommands.contains(permission);
    }

    public boolean canUse(ICommand command)
    {
        return hasPermission(command.getClass().getName());
    }
}
