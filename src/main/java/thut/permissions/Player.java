package thut.permissions;

import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;

public class Player extends PermissionsHolder
{
    public UUID         id;
    public List<String> groups  = Lists.newArrayList();
    public List<Group>  _groups = Lists.newArrayList();

    public Player()
    {
    }

    public boolean addGroup(Group group)
    {
        if (groups.contains(group.name)) return false;
        groups.add(group.name);
        _groups.add(group);
        return true;
    }

    public boolean removeGroup(Group group)
    {
        if (!_groups.contains(group)) return false;
        groups.remove(group.name);
        _groups.remove(group);
        return true;
    }

    @Override
    public boolean isAllowed(String permission)
    {
        for (Group group : _groups)
            if (group.isAllowed(permission)) return true;
        return super.isAllowed(permission);
    }

    @Override
    public boolean isDenied(String permission)
    {
        for (Group group : _groups)
            if (group.isDenied(permission)) return true;
        return super.isDenied(permission);
    }

    @Override
    public boolean isAll_non_op()
    {
        return false;
    }
}
