package thut.perms.management;

import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

public class Group extends PermissionsHolder
{
    public Set<UUID> members = Sets.newHashSet();

    public Group(final String name)
    {
        this.name = name;
    }

    @Override
    public void onUpdated(final MinecraftServer server)
    {
        for (final UUID id : this.members)
        {
            final ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) server.getCommands().sendCommands(player);
        }
    }
}
