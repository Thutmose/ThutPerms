package thut.permissions;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.minecraft.command.ICommand;

public abstract class PermissionsHolder
{
    public boolean        all             = false;
    public Set<String>    allowedCommands = Sets.newHashSet();
    private List<Pattern> wildCards       = Lists.newArrayList();
    private boolean       init            = false;

    private void init()
    {
        init = true;
        for (String s : allowedCommands)
        {
            if (s.endsWith("*"))
            {
                wildCards.add(Pattern.compile(s));
            }
        }
    }

    public boolean hasPermission(String permission)
    {
        if (all) return true;
        if (!init) init();
        for (Pattern pattern : wildCards)
        {
            if (pattern.matcher(permission).matches()) return true;
        }
        return allowedCommands.contains(permission);
    }

    public boolean canUse(ICommand command)
    {
        return hasPermission(command.getClass().getName());
    }

}
