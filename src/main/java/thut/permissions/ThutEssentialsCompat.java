package thut.permissions;

import net.minecraft.entity.player.EntityPlayer;
import thut.essentials.ThutEssentials;
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

}
