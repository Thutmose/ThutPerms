package thut.permissions;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import thut.essentials.events.NameEvent;

public class ThutEssentialsCompat
{

    public ThutEssentialsCompat()
    {
    }

    @SubscribeEvent
    public void NameEvent(NameEvent evt)
    {
        Group g = GroupManager._instance.getPlayerGroup(evt.toName.getUniqueID());
        String name = evt.getName();
        // Apply main group suffix first.
        if (g != null)
        {
            if (!g.prefix.isEmpty()) name = g.prefix + name;
        }

        Player player = GroupManager._instance._playerIDMap.get(evt.toName.getUniqueID());
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
