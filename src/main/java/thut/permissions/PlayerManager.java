package thut.permissions;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

public class PlayerManager
{
    private Map<UUID, Player> _playerIDMap = Maps.newHashMap();

    public MinecraftServer    _server;

    public GroupManager       _manager;

    public PlayerManager(GroupManager manager)
    {
        this._manager = manager;
        ThutPerms.logger.log(Level.INFO, "Initializing Player Manager.");
    }

    public void init(Player player)
    {
        _playerIDMap.put(player.id, player);

        // Setup groups.
        for (String name : player.groups)
        {
            Group group = _manager._groupNameMap.get(name);
            if (group != null) player._groups.add(group);
        }

        // Set up parent.
        if (player.parentName != null)
        {
            Group group = _manager._groupNameMap.get(player.parentName);
            player._parent = group;
            // No need to store parent name for the group we are specifically
            if (player._parent == null || group.members.contains(player.id)) player.parentName = null;
        }
        if (player._parent == null)
        {
            player._parent = _manager.getPlayerGroup(player.id);
        }
    }

    public Player getPlayer(UUID id)
    {
        Player player = _playerIDMap.get(id);
        if (player == null) player = createPlayer(id);
        return player;
    }

    public Group getPlayerGroup(UUID id)
    {
        Group ret = _manager._groupIDMap.get(id);
        if (ret == null)
        {
            if (_server.getPlayerList().getOppedPlayers()
                    .getEntry(new GameProfile(id, null)) != null) { return _manager.mods; }
            return _manager.initial;
        }
        return ret;
    }

    public boolean hasPermission(UUID id, String perm)
    {
        Group g = getPlayerGroup(id);
        Player player = _playerIDMap.get(id);
        boolean canPlayerUse = (player != null ? player.hasPermission(perm) : false);

        // Check if that player is specifically denied the perm.
        if (player != null && player.isDenied(perm)) return false;

        return g.hasPermission(perm) || canPlayerUse;
    }

    public void savePlayer(UUID id)
    {
        Player player = _playerIDMap.get(id);
        if (player == null) return;
        File playerFile = new File(ThutPerms.folder_players, id + ".json");
        Gson gson = new GsonBuilder().addSerializationExclusionStrategy(ThutPerms.exclusion).setPrettyPrinting()
                .create();
        Predicate<String> nonnull = new Predicate<String>()
        {
            @Override
            public boolean test(String t)
            {
                return t == null || t.isEmpty();
            }
        };

        player.getAllowedCommands().removeIf(nonnull);
        player.getBannedCommands().removeIf(nonnull);

        Collections.sort(player.getAllowedCommands());
        Collections.sort(player.getBannedCommands());

        try
        {
            FileUtils.writeStringToFile(playerFile, gson.toJson(player), "UTF-8");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void unloadPlayer(EntityPlayer player_)
    {
        UUID id = player_.getUniqueID();
        Player player = _playerIDMap.get(id);
        if (player == null) return;
        savePlayer(id);
        _playerIDMap.remove(id);
    }

    public boolean loadPlayer(UUID id)
    {
        File playerFile = new File(ThutPerms.folder_players, id + ".json");
        if (!playerFile.exists()) return false;
        String json = null;
        try
        {
            Gson gson = new GsonBuilder().addDeserializationExclusionStrategy(ThutPerms.exclusion).setPrettyPrinting()
                    .create();
            json = FileUtils.readFileToString(playerFile, "UTF-8");
            Player player = gson.fromJson(json, Player.class);
            player.id = id;
            init(player);
            return true;
        }
        catch (Throwable e)
        {
            e.printStackTrace();
            System.err.println("\n" + json);
            System.out.println(e);
            return false;
        }
    }

    public Player createPlayer(UUID id)
    {
        if (loadPlayer(id)) return _playerIDMap.get(id);
        Player player = new Player();
        player.id = id;
        init(player);
        return player;
    }

    public void createPlayer(EntityPlayer player_)
    {
        createPlayer(player_.getUniqueID());
    }

}
