package thut.permissions;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class GroupManager
{
    public static GroupManager instance;

    public Map<UUID, Group>    groupIDMap   = Maps.newHashMap();
    public Map<UUID, Player>   playerIDMap  = Maps.newHashMap();
    public Map<String, Group>  groupNameMap = Maps.newHashMap();
    public HashSet<Group>      groups       = Sets.newHashSet();
    public HashSet<Player>     players      = Sets.newHashSet();

    public Group               initial      = new Group("default");
    public Group               mods         = new Group("mods");

    public GroupManager()
    {
        ThutPerms.logger.log(Level.INFO, "Initializing Group Manager.");
    }

    public void init()
    {
        if (initial == null) initial = new Group("default");
        for (UUID id : initial.members)
        {
            groupIDMap.put(id, initial);
        }
        groupNameMap.put(initial.name, initial);
        if (mods == null) mods = new Group("mods");
        for (UUID id : mods.members)
        {
            groupIDMap.put(id, mods);
        }
        groupNameMap.put(mods.name, mods);
        for (Group g : groups)
        {
            if (g.name.isEmpty()) g.name = "unnamed" + new Random().nextFloat();
            groupNameMap.put(g.name, g);
            for (UUID id : g.members)
            {
                initial.members.remove(id);
                mods.members.remove(id);
                groupIDMap.put(id, g);
            }
        }

        // Refeshes players in groups to ensure that there is only 1 group with
        // each player, this cleans up some issues with badly formatted
        // permissions files
        for (UUID id : groupIDMap.keySet())
        {
            ThutPerms.addToGroup(id, groupIDMap.get(id).name);
        }

        mods.all = true;
        groupNameMap.put(initial.name, initial);
        groupNameMap.put(mods.name, mods);
        for (Player player : players)
        {
            playerIDMap.put(player.id, player);
            // Set up parent.
            if (player.parentName != null)
            {
                player.parent = groupNameMap.get(player.parentName);
                if (player.parent == null) player.parentName = null;
            }
        }
        // Set up parents.
        for (Group g : groups)
        {
            if (g.parentName != null)
            {
                g.parent = groupNameMap.get(g.parentName);
                if (g.parent == null) g.parentName = null;
            }
        }
    }

    public Player createPlayer(UUID id)
    {
        Player player = new Player();
        player.id = id;
        players.add(player);
        playerIDMap.put(id, player);
        return player;
    }

    public Group getPlayerGroup(UUID id)
    {
        Group ret = groupIDMap.get(id);
        if (ret == null)
        {
            if (initial == null) initial = new Group("default");
            ret = initial;
            groupIDMap.put(id, ret);
        }
        return ret;
    }

    public boolean hasPermission(UUID id, String perm)
    {
        Group g = GroupManager.instance.getPlayerGroup(id);
        Player player = GroupManager.instance.playerIDMap.get(id);
        boolean canPlayerUse = (player != null ? player.hasPermission(perm) : false);

        // Check if that player is specifically denied the perm.
        if (player != null && player.isDenied(perm)) return false;

        return g.hasPermission(perm) || canPlayerUse;
    }

}
