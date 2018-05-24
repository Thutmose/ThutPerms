package thut.permissions;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;

import net.minecraft.command.ICommand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.UserListOpsEntry;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.context.IContext;

public class PermissionsManager implements IPermissionHandler
{
    public boolean                                               SPDiabled            = true;
    private static final HashMap<String, DefaultPermissionLevel> PERMISSION_LEVEL_MAP = new HashMap<String, DefaultPermissionLevel>();
    private static final HashMap<String, String>                 DESCRIPTION_MAP      = new HashMap<String, String>();

    @Override
    public void registerNode(String node, DefaultPermissionLevel level, String desc)
    {
        PERMISSION_LEVEL_MAP.put(node, level);
        if (!desc.isEmpty())
        {
            DESCRIPTION_MAP.put(node, desc);
        }
    }

    @Override
    public Collection<String> getRegisteredNodes()
    {
        return Collections.unmodifiableSet(PERMISSION_LEVEL_MAP.keySet());
    }

    @Override
    public boolean hasPermission(GameProfile profile, String node, @Nullable IContext context)
    {
        if (SPDiabled && FMLCommonHandler.instance().getMinecraftServerInstance().isSinglePlayer()) return true;
        return GroupManager.instance.hasPermission(profile.getId(), node);
    }

    @Override
    public String getNodeDescription(String node)
    {
        String desc = DESCRIPTION_MAP.get(node);
        return desc == null ? "" : desc;
    }

    /** @return The default permission level of a node. If the permission isn't
     *         registred, it will return NONE */
    public DefaultPermissionLevel getDefaultPermissionLevel(String node)
    {
        DefaultPermissionLevel level = PERMISSION_LEVEL_MAP.get(node);
        return level == null ? DefaultPermissionLevel.NONE : level;
    }

    boolean on = false;

    @SubscribeEvent
    void commandUseEvent(CommandEvent event)
    {
        if (!event.getSender().getServer().isDedicatedServer()) return;
        if (event.getSender() instanceof EntityPlayerMP
                && !canUse(event.getCommand(), (EntityPlayer) event.getSender()))
        {
            event.getSender().sendMessage(
                    new TextComponentString("You do not have permission to use " + event.getCommand().getName()));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void PlayerLoggin(PlayerLoggedInEvent evt)
    {
        EntityPlayer entityPlayer = evt.player;
        if (GroupManager.instance.groupIDMap.get(entityPlayer.getUniqueID()) == null)
        {
            UserListOpsEntry userentry = ((EntityPlayerMP) entityPlayer).mcServer.getPlayerList().getOppedPlayers()
                    .getEntry(entityPlayer.getGameProfile());
            if (userentry != null && userentry.getPermissionLevel() >= 4)
            {
                GroupManager.instance.mods.members.add(entityPlayer.getUniqueID());
                GroupManager.instance.groupIDMap.put(entityPlayer.getUniqueID(), GroupManager.instance.mods);
                ThutPerms.savePerms();
            }
            else
            {
                GroupManager.instance.initial.members.add(entityPlayer.getUniqueID());
                GroupManager.instance.groupIDMap.put(entityPlayer.getUniqueID(), GroupManager.instance.initial);
                ThutPerms.savePerms();
            }
            entityPlayer.refreshDisplayName();
        }
    }

    private boolean canUse(ICommand command, EntityPlayer sender)
    {
        if (GroupManager.instance.playerIDMap.containsKey(sender.getUniqueID()))
        {
            Player player = GroupManager.instance.playerIDMap.get(sender.getUniqueID());
            if (player.canUse(command)) return true;
        }
        return GroupManager.instance.getPlayerGroup(sender.getUniqueID()).canUse(command);
    }
}
