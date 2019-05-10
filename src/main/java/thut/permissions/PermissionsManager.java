package thut.permissions;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;

import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.context.IContext;
import thut.permissions.util.CommandWrapper;

public class PermissionsManager implements IPermissionHandler
{
    public static final GameProfile                              testProfile          = new GameProfile(
            new UUID(1234567987, 123545787), "_permtest_");
    public static EntityPlayerMP                                 testPlayer;
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
        if (FMLCommonHandler.instance().getMinecraftServerInstance() == null) return true;
        if (SPDiabled && FMLCommonHandler.instance().getMinecraftServerInstance().isSinglePlayer()) return true;
        if (GroupManager.get_instance() == null)
        {
            ThutPerms.logger.log(Level.WARNING, node + " is being checked before load!");
            Thread.dumpStack();
            return getDefaultPermissionLevel(node) == DefaultPermissionLevel.ALL;
        }
        if (ThutPerms.debug) ThutPerms.logger.log(Level.INFO, "permnode: " + node + " " + profile + " "
                + GroupManager.get_instance().hasPermission(profile.getId(), node));
        return GroupManager.get_instance().hasPermission(profile.getId(), node);
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

    public void onServerStarted(FMLServerStartedEvent event)
    {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || (SPDiabled && !server.isDedicatedServer())) return;

        testPlayer = new EntityPlayerMP(server, server.getWorld(0), testProfile,
                new PlayerInteractionManager(server.getEntityWorld()));
        ThutPerms.setAnyCommandUse(server, false);
        Map<String, ICommand> toWrap = Maps.newHashMap();
        for (String name : server.getCommandManager().getCommands().keySet())
        {
            String node = "command." + name;
            if (DESCRIPTION_MAP.containsKey(node))
            {
                node = "command." + node;
            }
            ICommand command = server.getCommandManager().getCommands().get(name);
            DefaultPermissionLevel level = command.checkPermission(server, testPlayer) ? DefaultPermissionLevel.ALL
                    : DefaultPermissionLevel.OP;
            registerNode(node, level,
                    "Autogenerated permission for " + name + " command, aliases: " + command.getAliases());
            if (level == DefaultPermissionLevel.OP) toWrap.put(command.getName(), command);
        }
        if (ThutPerms.wrapOpCommands) wrapCommands(FMLCommonHandler.instance().getMinecraftServerInstance(), toWrap);
        ThutPerms.setAnyCommandUse(server, ThutPerms.allCommandUse);
    }

    private void wrapCommands(MinecraftServer server, Map<String, ICommand> toWrap)
    {
        // shouldn't be null, but might be if something goes funny connecting to
        // servers.
        if (server == null) return;
        ThutPerms.logger.fine("Wrapping Commands");

        CommandHandler ch = (CommandHandler) server.getCommandManager();

        // Remove all the commands we are about to wrap.
        Set<ICommand> commandSet = ch.commandSet;
        commandSet.removeAll(toWrap.values());

        Map<String, ICommand> wrappers = Maps.newHashMap();

        // Wrap and replace the commands.
        Map<String, ICommand> commandMap = ch.getCommands();

        for (Entry<String, ICommand> entry : commandMap.entrySet())
        {
            ICommand value = entry.getValue();
            // We need to wrap this command.
            if (toWrap.containsKey(value.getName()))
            {
                ICommand wrap;
                // Get wrapper or make new and add it to the set.
                if (wrappers.containsKey(value.getName())) wrap = wrappers.get(value.getName());
                else
                {
                    wrappers.put(value.getName(), wrap = new CommandWrapper(value));
                    commandSet.add(wrap);
                }
                // Replace the map entry.
                entry.setValue(wrap);
                // Log what we did.
                ThutPerms.logger.fine("Wrapped " + value.getName());
            }
        }

    }

    @SubscribeEvent
    void commandUseEvent(CommandEvent event)
    {
        if (event.getSender().getServer() == null || (SPDiabled && !event.getSender().getServer().isDedicatedServer()))
            return;
        if (event.getSender() instanceof EntityPlayerMP
                && !canUse(event.getCommand(), (EntityPlayer) event.getSender()))
        {
            event.getSender().sendMessage(
                    new TextComponentString("You do not have permission to use " + event.getCommand().getName()));
            event.setCanceled(true);
        }
    }

    private boolean canUse(ICommand command, EntityPlayer sender)
    {
        if (ThutPerms.debug) ThutPerms.logger.log(Level.INFO, "command use: " + command.getName() + ", "
                + command.getAliases() + ", " + sender.getGameProfile() + " " + command.getClass());
        Player player = GroupManager.get_instance()._manager.getPlayer(sender.getUniqueID());
        return player.canUse(command);
    }
}
