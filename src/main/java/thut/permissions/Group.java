package thut.permissions;

import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;

import net.minecraftforge.server.permission.DefaultPermissionLevel;

public class Group extends PermissionsHolder
{
    public String    name;
    public String    prefix  = "";
    public String    suffix  = "";
    public Set<UUID> members = Sets.newHashSet();

    public Group(String name)
    {
        this.name = name;
        for (String node : ThutPerms.manager.getRegisteredNodes())
        {
            if (ThutPerms.manager.getDefaultPermissionLevel(node) == DefaultPermissionLevel.ALL)
            {
                allowedCommands.add(node);
            }
        }
    }
}
