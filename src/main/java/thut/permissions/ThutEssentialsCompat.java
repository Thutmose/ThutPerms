package thut.permissions;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import thut.essentials.ThutEssentials;
import thut.essentials.events.NameEvent;
import thut.essentials.util.IPermissionHandler;

public class ThutEssentialsCompat implements IPermissionHandler
{

    public ThutEssentialsCompat()
    {
        ThutEssentials.perms = this;
    }

    @Override
    public boolean hasPermission(EntityPlayer player, String permission)
    {
        return GroupManager.instance.hasPermission(player.getUniqueID(), permission);
    }

    @SubscribeEvent
    public void NameEvent(NameEvent evt)
    {
        Group g = GroupManager.instance.getPlayerGroup(evt.toName.getUniqueID());
        String name = evt.getName();
        // Apply main group suffix first.
        if (g != null)
        {
            if (!g.prefix.isEmpty()) name = g.prefix + name;
        }

        Player player = GroupManager.instance._playerIDMap.get(evt.toName.getUniqueID());
        if (player != null)
        {
            // Prefixes in order in list
            for (Group g1 : player._groups)
            {
                if (!g1.prefix.isEmpty()) name = g1.prefix + name;
            }
            // Suffixes in order in list
            for (Group g1 : player._groups)
            {
                if (!g1.suffix.isEmpty()) name = name + g1.suffix;
            }
        }
        // Main group suffic last.
        if (g != null)
        {
            if (!g.suffix.isEmpty()) name = name + g.suffix;
        }
        evt.setName(name);
    }

}
