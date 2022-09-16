package thut.perms.management;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import thut.perms.Perms;

public class PlayerManager
{
    private final Map<UUID, Player> _playerIDMap = Maps.newHashMap();

    public GroupManager _manager;

    public PlayerManager(final GroupManager manager)
    {
        this._manager = manager;
        Perms.LOGGER.info("Initializing Player Manager.");
    }

    public void init(final Player player)
    {
        this._playerIDMap.put(player.id, player);

        // Setup groups.
        for (final String name : player.groups)
        {
            final Group group = this._manager._groupNameMap.get(name);
            if (group != null) player._groups.add(group);
        }

        // Set up parent.
        if (player.parentName != null)
        {
            final Group group = this._manager._groupNameMap.get(player.parentName);
            player._parent = group;
            // No need to store parent name for the group we are specifically
            if (player._parent == null || group.members.contains(player.id)) player.parentName = null;
        }
        if (player._parent == null) player._parent = this._manager.getPlayerGroup(player.id);
    }

    public Player getPlayer(final UUID id)
    {
        Player player = this._playerIDMap.get(id);
        if (player == null) player = this.createPlayer(id);
        return player;
    }

    public Group getPlayerGroup(final UUID id)
    {
        final Group ret = this._manager._groupIDMap.get(id);
        if (ret == null)
        {
            if (Perms.getServer().getPlayerList().getOps().get(new GameProfile(id, null)) != null)
                return this._manager.mods;
            return this._manager.initial;
        }
        return ret;
    }

    public boolean hasPermission(final UUID id, final PermissionNode<Boolean> perm)
    {
        final Group g = this.getPlayerGroup(id);
        final Player player = this._playerIDMap.get(id);
        final boolean canPlayerUse = player != null ? player.hasPermission(perm) : false;

        // Check if that player is specifically denied the perm.
        if (player != null && player.isDenied(perm)) return false;

        return g.hasPermission(perm) || canPlayerUse;
    }

    public void savePlayer(final UUID id)
    {
        final Player player = this._playerIDMap.get(id);
        if (player == null) return;
        final File playerFile = new File(Perms.folder_players, id + ".json");
        final Gson gson = new GsonBuilder().addSerializationExclusionStrategy(Perms.exclusion).setPrettyPrinting()
                .create();
        final Predicate<String> nonnull = t -> t == null || t.isEmpty();

        player.getAllowedCommands().removeIf(nonnull);
        player.getBannedCommands().removeIf(nonnull);

        Collections.sort(player.getAllowedCommands());
        Collections.sort(player.getBannedCommands());

        try
        {
            FileUtils.writeStringToFile(playerFile, gson.toJson(player), "UTF-8");
        }
        catch (final IOException e)
        {
            Perms.LOGGER.error("Error saving player perms", e);
        }
    }

    public void unloadPlayer(final ServerPlayer player_)
    {
        this.unloadPlayer(player_.getUUID());
    }

    public void unloadPlayer(final UUID id)
    {
        final Player player = this._playerIDMap.get(id);
        if (player == null) return;
        this.savePlayer(id);
        this._playerIDMap.remove(id);
    }

    public boolean loadPlayer(final UUID id)
    {
        final File playerFile = new File(Perms.folder_players, id + ".json");
        if (!playerFile.exists()) return false;
        String json = null;
        try
        {
            final Gson gson = new GsonBuilder().addDeserializationExclusionStrategy(Perms.exclusion).setPrettyPrinting()
                    .create();
            json = FileUtils.readFileToString(playerFile, "UTF-8");
            final Player player = gson.fromJson(json, Player.class);
            player.id = id;
            this.init(player);
            return true;
        }
        catch (final Throwable e)
        {
            Perms.LOGGER.error("Error reading player {}", json);
            Perms.LOGGER.error(e);
            return false;
        }
    }

    public Player createPlayer(final UUID id)
    {
        if (this.loadPlayer(id)) return this._playerIDMap.get(id);
        final Player player = new Player();
        player.id = id;
        this.init(player);
        return player;
    }

    public void createPlayer(final ServerPlayer player_)
    {
        this.createPlayer(player_.getUUID());
    }

}
