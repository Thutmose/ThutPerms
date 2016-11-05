package thut.permissions;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.command.ICommand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerList;
import net.minecraft.server.management.UserListOpsEntry;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import thut.permissions.commands.CommandManager;
import thut.permissions.util.SpawnProtector;

@Mod(modid = ThutPerms.MODID, name = "Thut Permissions", version = ThutPerms.VERSION, dependencies = "after:worldedit", updateJSON = ThutPerms.UPDATEURL, acceptableRemoteVersions = "*", acceptedMinecraftVersions = ThutPerms.MCVERSIONS)
public class ThutPerms
{
    public static final String MODID         = "thutperms";
    public static final String VERSION       = "0.1.1";
    public static final String UPDATEURL     = "";

    public final static String MCVERSIONS    = "[1.9.4]";

    public static boolean      allCommandUse = false;
    public static File         configFile    = null;

    static ExclusionStrategy   exclusion     = new ExclusionStrategy()
                                             {
                                                 @Override
                                                 public boolean shouldSkipField(FieldAttributes f)
                                                 {
                                                     String name = f.getName();
                                                     return name.equals("groupIDMap") || name.equals("groupNameMap")
                                                             || name.equals("playerIDMap");
                                                 }

                                                 @Override
                                                 public boolean shouldSkipClass(Class<?> clazz)
                                                 {
                                                     return false;
                                                 }
                                             };

    @EventHandler
    public void preInit(FMLPreInitializationEvent e)
    {
        Configuration config = new Configuration(configFile = e.getSuggestedConfigurationFile());
        config.load();
        allCommandUse = config.getBoolean("allCommandUse", Configuration.CATEGORY_GENERAL, false,
                "Can any player use OP commands if their group is allowed to?");
        config.save();
        MinecraftForge.EVENT_BUS.register(new SpawnProtector());
    }

//    @Optional.Method(modid = "worldedit")
//    @EventHandler
//    public void serverAboutToStart(FMLServerStartingEvent event)
//    {
//        thut.permissions.WorldEditPermissions worldEditSupport = new thut.permissions.WorldEditPermissions();
//        com.sk89q.worldedit.forge.ForgeWorldEdit.inst.setPermissionsProvider(worldEditSupport);
//        System.out.println("REGISTERING WORLD EDIT SUPPORT");
//    }

    @EventHandler
    public void serverLoad(FMLServerStartingEvent event)
    {
        loadPerms();
        if (GroupManager.instance.initial == null)
        {
            GroupManager.instance.initial = new Group("default");
            savePerms();
        }
        if (GroupManager.instance.mods == null)
        {
            GroupManager.instance.mods = new Group("mods");
            GroupManager.instance.mods.all = true;
            savePerms();
        }
        new CommandManager(event);
        MinecraftForge.EVENT_BUS.register(this);

        if (allCommandUse)
        {
            Field f = ReflectionHelper.findField(PlayerList.class, "commandsAllowedForAll", "field_72407_n", "t");
            f.setAccessible(true);
            try
            {
                f.set(event.getServer().getPlayerList(), true);
            }
            catch (IllegalArgumentException | IllegalAccessException e)
            {
                e.printStackTrace();
            }
        }
    }

    @SubscribeEvent
    void commandUseEvent(CommandEvent event)
    {
        if (!event.getSender().getServer().isDedicatedServer()) return;
        if (event.getSender() instanceof EntityPlayer && !canUse(event.getCommand(), (EntityPlayer) event.getSender()))
        {
            event.getSender().addChatMessage(new TextComponentString(
                    "You do not have permission to use /" + event.getCommand().getCommandName()));
            event.setCanceled(true);
        }
    }

    @Optional.Method(modid = "thutessentials")
    @SubscribeEvent
    public void NameEvent(thut.essentials.events.NameEvent evt)
    {
        Group g = GroupManager.instance.getPlayerGroup(evt.toName.getUniqueID());
        if (g == null) return;
        String name = evt.getName();
        if (!g.prefix.isEmpty()) name = g.prefix + " " + name;
        if (!g.suffix.isEmpty()) name = name + " " + g.suffix;
        evt.setName(name);
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
                savePerms();
            }
            else
            {
                GroupManager.instance.initial.members.add(entityPlayer.getUniqueID());
                GroupManager.instance.groupIDMap.put(entityPlayer.getUniqueID(), GroupManager.instance.initial);
                savePerms();
            }
            entityPlayer.refreshDisplayName();
        }
    }

    public static void loadPerms()
    {
        String folder = FMLCommonHandler.instance().getMinecraftServerInstance().getFolderName();
        File file = FMLCommonHandler.instance().getSavesDirectory();
        File saveFolder = new File(file, folder);
        File permsFolder = new File(saveFolder, "permissions");
        if (!permsFolder.exists()) permsFolder.mkdirs();
        File permsFile = new File(permsFolder, "permissions.json");
        if (permsFile.exists())
        {
            try
            {
                Gson gson = new GsonBuilder().addDeserializationExclusionStrategy(exclusion).setPrettyPrinting()
                        .create();
                String json = FileUtils.readFileToString(permsFile, "UTF-8");
                GroupManager.instance = gson.fromJson(json, GroupManager.class);
                GroupManager.instance.init();
                savePerms();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            GroupManager.instance = new GroupManager();
            GroupManager.instance.init();
            savePerms();
        }
    }

    public static void savePerms()
    {
        String folder = FMLCommonHandler.instance().getMinecraftServerInstance().getFolderName();
        File file = FMLCommonHandler.instance().getSavesDirectory();
        File saveFolder = new File(file, folder);
        File permsFolder = new File(saveFolder, "permissions");
        if (!permsFolder.exists()) permsFolder.mkdirs();
        File permsFile = new File(permsFolder, "permissions.json");
        try
        {
            Gson gson = new GsonBuilder().addSerializationExclusionStrategy(exclusion).setPrettyPrinting().create();
            FileUtils.writeStringToFile(permsFile, gson.toJson(GroupManager.instance), "UTF-8");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static Group addGroup(String name)
    {
        Group ret = new Group(name);
        GroupManager.instance.groupNameMap.put(name, ret);
        GroupManager.instance.groups.add(ret);
        return ret;
    }

    public static void addToGroup(UUID id, String name)
    {
        Group group = GroupManager.instance.groupNameMap.get(name);
        if (group == null) group = GroupManager.instance.initial;
        group.members.add(id);
        GroupManager.instance.groupIDMap.put(id, group);
    }

    public static Group getGroup(String name)
    {
        return GroupManager.instance.groupNameMap.get(name);
    }

    private boolean canUse(ICommand command, EntityPlayer sender)
    {
        UUID id = sender.getUniqueID();
        return GroupManager.instance.hasPermission(id, command.getClass().getName());
    }
}
