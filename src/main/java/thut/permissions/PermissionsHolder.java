package thut.permissions;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.command.ICommand;

public abstract class PermissionsHolder
{
    public boolean           all             = false;
    public List<String>      allowedCommands = Lists.newArrayList();
    public List<String>      bannedCommands  = Lists.newArrayList();
    public String            parentName      = null;
    public PermissionsHolder parent;
    protected List<String>   whiteWildCards;
    protected List<String>   blackWildCards;
    public boolean           init            = false;

    private void init()
    {
        whiteWildCards = Lists.newArrayList();
        blackWildCards = Lists.newArrayList();
        if (allowedCommands == null) allowedCommands = Lists.newArrayList();
        if (bannedCommands == null) bannedCommands = Lists.newArrayList();
        init = true;
        for (String s : allowedCommands)
        {
            if (s.endsWith("*"))
            {
                whiteWildCards.add(s.substring(0, s.length() - 1));
            }
            else if (s.startsWith("*"))
            {
                whiteWildCards.add(s.substring(1));
            }
        }
        for (String s : bannedCommands)
        {
            if (s.endsWith("*"))
            {
                blackWildCards.add(s.substring(0, s.length() - 1));
            }
            else if (s.startsWith("*"))
            {
                blackWildCards.add(s.substring(1));
            }
        }
    }

    public boolean isDenied(String permission)
    {
        if (parent != null && parent.isDenied(permission)) return true;
        if (!init || blackWildCards == null || bannedCommands == null) init();
        for (String pattern : blackWildCards)
        {
            if (permission.startsWith(pattern)) return true;
            else if (permission.matches(pattern)) return true;
        }
        if (bannedCommands.contains(permission)) return true;
        return false;
    }

    public boolean isAllowed(String permission)
    {
        if (parent != null && parent.isAllowed(permission)) return true;
        if (all) return true;
        if (!init || whiteWildCards == null || allowedCommands == null) init();
        for (String pattern : whiteWildCards)
        {
            if (permission.startsWith(pattern)) return true;
            else if (permission.matches(pattern)) return true;
        }
        return allowedCommands.contains(permission);
    }

    public boolean hasPermission(String permission)
    {
        // Check if permission is specifically denied.
        if (isDenied(permission)) return false;
        // check if permission is allowed.
        return isAllowed(permission);
    }

    public boolean canUse(ICommand command)
    {
        // Check if the command falls under any denied rules.
        if (isDenied("command." + command.getName())) return false;
        for (String alias : command.getAliases())
        {
            if (isDenied("command." + alias)) return false;
        }
        if (ThutPerms.customCommandPerms.containsKey(command.getName())
                && isDenied(ThutPerms.customCommandPerms.get(command.getName())))
            return false;
        if (isDenied(command.getClass().getName())) return false;

        // Then check if it is allowed.
        if (isAllowed("command." + command.getName())) return true;
        for (String alias : command.getAliases())
        {
            if (isAllowed("command." + alias)) return true;
        }
        if (ThutPerms.customCommandPerms.containsKey(command.getName())
                && isAllowed(ThutPerms.customCommandPerms.get(command.getName())))
            return true;
        return isAllowed(command.getClass().getName());
    }

}
