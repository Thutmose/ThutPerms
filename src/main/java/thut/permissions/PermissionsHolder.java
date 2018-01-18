package thut.permissions;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.minecraft.command.ICommand;

public abstract class PermissionsHolder
{
    public boolean       all             = false;
    public Set<String>   allowedCommands = Sets.newHashSet();
    private List<String> wildCards       = Lists.newArrayList();
    private boolean      init            = false;

    private void init()
    {
        init = true;
        for (String s : allowedCommands)
        {
            if (s.endsWith("*"))
            {
                wildCards.add(s.substring(0, s.length() - 1));
            }
            else if (s.startsWith("*"))
            {
                wildCards.add(s.substring(1));
            }
        }
    }

    public boolean hasPermission(String permission)
    {
        if (all) return true;
        if (!init) init();
        for (String pattern : wildCards)
        {
            if (permission.startsWith(pattern)) return true;
            else if (permission.matches(pattern)) return true;
        }
        return allowedCommands.contains(permission);
    }

    public boolean canUse(ICommand command)
    {
        return hasPermission(command.getClass().getName());
    }

}
