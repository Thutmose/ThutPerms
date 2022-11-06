package thut.perms.management;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import thut.perms.management.PermNodes.DefaultPermissionLevel;

public abstract class PermissionsHolder
{
    public String name = "";
    public String prefix = "";
    public String suffix = "";
    public boolean all = false;
    public boolean all_non_op = true;
    public String parentName = null;

    public List<String> allowedCommands = Lists.newArrayList();
    public List<String> bannedCommands = Lists.newArrayList();

    public Map<String, Integer> permNumbers = Maps.newHashMap();

    public Map<String, String> permStrings = Maps.newHashMap();
    public Map<String, List<String>> permStringSets = Maps.newHashMap();

    public PermissionsHolder _parent;
    protected List<String> _whiteWildCards;
    protected List<String> _blackWildCards;

    // This one has had the permStringSets merged into it.
    protected Map<String, String> _permStrings = Maps.newHashMap();

    public boolean _init = false;

    private void init()
    {
        this._whiteWildCards = Lists.newArrayList();
        this._blackWildCards = Lists.newArrayList();
        if (this.allowedCommands == null) this.allowedCommands = Lists.newArrayList();
        if (this.bannedCommands == null) this.bannedCommands = Lists.newArrayList();
        this._init = true;
        for (final String s : this.allowedCommands)
            if (s.endsWith("*")) this._whiteWildCards.add(s.substring(0, s.length() - 1));
            else if (s.startsWith("*")) this._whiteWildCards.add(s.substring(1));
        for (final String s : this.bannedCommands)
            if (s.endsWith("*")) this._blackWildCards.add(s.substring(0, s.length() - 1));
            else if (s.startsWith("*")) this._blackWildCards.add(s.substring(1));

        for (var entry : permStrings.entrySet())
        {
            _permStrings.compute(entry.getKey(), (key, oldValue) -> {
                if (oldValue == null)
                {
                    return entry.getValue();
                }
                String value = oldValue;
                value = entry.getValue() + "," + value;
                return value;
            });
        }
        for (var entry : permStringSets.entrySet())
        {
            _permStrings.compute(entry.getKey(), (key, value) -> {
                if (value == null) value = "";
                for (var str : entry.getValue()) value = str + "," + value;
                return value;
            });
        }
    }

    public abstract void onUpdated(MinecraftServer server);

    public List<String> getAllowedCommands()
    {
        if (this.allowedCommands == null) this.allowedCommands = Lists.newArrayList();
        return this.allowedCommands;
    }

    public List<String> getBannedCommands()
    {
        if (this.bannedCommands == null) this.bannedCommands = Lists.newArrayList();
        return this.bannedCommands;
    }

    public boolean isDenied(final PermissionNode<Boolean> permission)
    {
        if (this._parent != null && this._parent.isDenied(permission)) return true;
        if (!this._init || this._blackWildCards == null || this.bannedCommands == null) this.init();
        for (final String pattern : this._blackWildCards) if (permission.getNodeName().startsWith(pattern)) return true;
        else if (permission.getNodeName().matches(pattern)) return true;
        if (this.bannedCommands.contains(permission.getNodeName())) return true;
        return false;
    }

    public boolean isAllowed(final PermissionNode<Boolean> permission)
    {
        if (this._parent != null && this._parent.isAllowed(permission)) return true;
        if (this.isAll()) return true;
        if (!this._init || this._whiteWildCards == null || this.allowedCommands == null) this.init();
        if (this.isAll_non_op()
                && PermissionsManager.getDefaultPermissionLevel(permission) == DefaultPermissionLevel.ALL)
            return true;
        for (final String pattern : this._whiteWildCards) if (permission.getNodeName().startsWith(pattern)) return true;
        else if (permission.getNodeName().matches(pattern)) return true;
        return this.allowedCommands.contains(permission.getNodeName());
    }

    public boolean hasPermission(final PermissionNode<Boolean> permission)
    {
        // Check if permission is specifically denied.
        if (this.isDenied(permission)) return false;
        // check if permission is allowed.
        return this.isAllowed(permission);
    }

    @Nullable
    public Integer getNumberPerm(final PermissionNode<Integer> permission)
    {
        return permNumbers.getOrDefault(permission.getNodeName(), null);
    }

    @Nullable
    public String getStringPerm(final PermissionNode<String> permission)
    {
        return _permStrings.getOrDefault(permission.getNodeName(), null);
    }

    public boolean isAll()
    {
        return this.all;
    }

    public void setAll(final boolean all)
    {
        this.all = all;
    }

    public boolean isAll_non_op()
    {
        return this.all_non_op;
    }

    public void setAll_non_op(final boolean all_non_op)
    {
        this.all_non_op = all_non_op;
    }

}
