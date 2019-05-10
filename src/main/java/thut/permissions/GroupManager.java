package thut.permissions;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;

import net.minecraft.server.MinecraftServer;

public class GroupManager
{
    private static GroupManager              _instance;

    /** Map of dimension id to group manager. */
    public static Map<Integer, GroupManager> _instanceMap  = Maps.newHashMap();

    public Map<UUID, Group>                  _groupIDMap   = Maps.newHashMap();
    public Map<String, Group>                _groupNameMap = Maps.newHashMap();
    public HashSet<Group>                    groups        = Sets.newHashSet();

    public Group                             initial       = new Group("default");
    public Group                             mods          = new Group("mods");

    public PlayerManager                     _manager      = null;
    public MinecraftServer                   _server;

    /** @return the _instance */
    public static GroupManager get_instance()
    {
        return _instance;
    }

    /** @param _instance
     *            the _instance to set */
    public static void set_instance(GroupManager _instance)
    {
        GroupManager._instance = _instance;
    }

    public GroupManager()
    {
        ThutPerms.logger.log(Level.INFO, "Initializing Group Manager.");
        _manager = new PlayerManager(this);
    }

    public void init()
    {
        if (initial == null) initial = new Group("default");
        _groupNameMap.put(initial.name, initial);
        if (mods == null) mods = new Group("mods");
        _groupNameMap.put(mods.name, mods);
        for (Group g : groups)
        {
            if (g.name.isEmpty()) g.name = "unnamed" + new Random().nextFloat();
            _groupNameMap.put(g.name, g);
            for (UUID id : g.members)
            {
                initial.members.remove(id);
                mods.members.remove(id);
                _groupIDMap.put(id, g);
            }
        }

        // Refeshes players in groups to ensure that there is only 1 group with
        // each player, this cleans up some issues with badly formatted
        // permissions files
        for (UUID id : _groupIDMap.keySet())
        {
            ThutPerms.addToGroup(id, _groupIDMap.get(id).name);
        }

        mods.setAll(true);
        _groupNameMap.put(initial.name, initial);
        _groupNameMap.put(mods.name, mods);

        // Set up parents.
        for (Group g : groups)
        {
            if (g.parentName != null)
            {
                g._parent = _groupNameMap.get(g.parentName);
                if (g._parent == null) g.parentName = null;
            }
        }
    }

    public Group getPlayerGroup(UUID id)
    {
        Group ret = _groupIDMap.get(id);
        if (ret == null)
        {
            if (_server.getPlayerList().getOppedPlayers().getEntry(new GameProfile(id, null)) != null) { return mods; }
            return initial;
        }
        return ret;
    }

    public boolean hasPermission(UUID id, String perm)
    {
        Group g = getPlayerGroup(id);
        Player player = _manager.getPlayer(id);
        boolean canPlayerUse = (player != null ? player.hasPermission(perm) : false);

        // Check if that player is specifically denied the perm.
        if (player != null && player.isDenied(perm)) return false;

        return g.hasPermission(perm) || canPlayerUse;
    }

}
