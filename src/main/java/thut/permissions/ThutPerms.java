package thut.permissions;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Maps;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.permissions.commands.CommandManager;
import thut.permissions.util.SpawnProtector;

@Mod(modid = ThutPerms.MODID, name = "Thut Permissions", version = ThutPerms.VERSION, dependencies = "after:worldedit", updateJSON = ThutPerms.UPDATEURL, acceptableRemoteVersions = "*", acceptedMinecraftVersions = ThutPerms.MCVERSIONS)
public class ThutPerms
{
    public static final String             MODID              = Reference.MODID;
    public static final String             VERSION            = Reference.VERSION;
    public static final String             UPDATEURL          = "";

    public final static String             MCVERSIONS         = "[1.9.4, 1.13]";

    public static boolean                  allCommandUse      = false;
    public static File                     configFile         = null;
    public static final PermissionsManager manager            = new PermissionsManager();
    public static Logger                   logger;
    public static Map<String, String>      customCommandPerms = Maps.newHashMap();

    static ExclusionStrategy               exclusion          = new ExclusionStrategy()
                                                              {
                                                                  @Override
                                                                  public boolean shouldSkipField(FieldAttributes f)
                                                                  {
                                                                      String name = f.getName();
                                                                      return name.equals("groupIDMap")
                                                                              || name.equals("groupNameMap")
                                                                              || name.equals("playerIDMap")
                                                                              || name.equals("whiteWildCards")
                                                                              || name.equals("blackWildCards")
                                                                              || name.equals("parent")
                                                                              || name.equals("init");
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
        logger = e.getModLog();
        Configuration config = new Configuration(configFile = e.getSuggestedConfigurationFile());
        config.load();
        allCommandUse = config.getBoolean("allCommandUse", Configuration.CATEGORY_GENERAL, false,
                "Can any player use OP commands if their group is allowed to?");
        String[] custom = config.getStringList("customCommandPerms", Configuration.CATEGORY_GENERAL,
                new String[] { "give:minecraft.command.give" },
                "Custom mappings for permissions, the default shows an example for the give command");
        manager.SPDiabled = config.getBoolean("singleplayerdisabled", Configuration.CATEGORY_GENERAL, true,
                "does this not do anything single player");
        for (String s : custom)
        {
            String[] args = s.split(":");
            customCommandPerms.put(args[0], args[1]);
        }

        for (Field f : CommandManager.class.getDeclaredFields())
        {
            try
            {
                String value = (String) f.get(null);
                String name = f.getName();
                f.set(config.getString(name, Configuration.CATEGORY_GENERAL, value, "Name for " + name + " command"),
                        value);
            }
            catch (IllegalArgumentException | IllegalAccessException e1)
            {
                e1.printStackTrace();
            }
        }

        config.save();
        MinecraftForge.EVENT_BUS.register(new SpawnProtector());
        PermissionAPI.setPermissionHandler(manager);
        MinecraftForge.EVENT_BUS.register(manager);
    }

    @Optional.Method(modid = "thutessentials")
    @EventHandler
    public void thutEssentialsCompat(FMLPreInitializationEvent e)
    {
        new thut.permissions.ThutEssentialsCompat();
    }

    @EventHandler
    public void serverLoad(FMLServerStartedEvent event)
    {
        manager.onServerStarted(event);
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
    }

    @EventHandler
    public void serverLoad(FMLServerStartingEvent event)
    {
        new CommandManager(event);
        MinecraftForge.EVENT_BUS.register(this);
        ThutPerms.setAnyCommandUse(event.getServer(), allCommandUse);
    }

    public static void setAnyCommandUse(MinecraftServer server, boolean enable)
    {
        Field f = ReflectionHelper.findField(PlayerList.class, "commandsAllowedForAll", "field_72407_n", "t");
        f.setAccessible(true);
        try
        {
            f.set(server.getPlayerList(), enable);
        }
        catch (IllegalArgumentException | IllegalAccessException e)
        {
            e.printStackTrace();
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
            Predicate<String> nonnull = new Predicate<String>()
            {
                @Override
                public boolean test(String t)
                {
                    return t != null && !t.isEmpty();
                }
            };
            for (Group group : GroupManager.instance.groups)
            {
                group.allowedCommands.removeIf(nonnull);
                group.bannedCommands.removeIf(nonnull);
                if (!group.allowedCommands.isEmpty()) Collections.sort(group.allowedCommands);
                if (!group.bannedCommands.isEmpty()) Collections.sort(group.bannedCommands);
            }
            for (Player player : GroupManager.instance.players)
            {
                player.allowedCommands.removeIf(nonnull);
                player.bannedCommands.removeIf(nonnull);
                if (!player.allowedCommands.isEmpty()) Collections.sort(player.allowedCommands);
                if (!player.bannedCommands.isEmpty()) Collections.sort(player.bannedCommands);
            }
            FileUtils.writeStringToFile(permsFile, gson.toJson(GroupManager.instance), "UTF-8");
        }
        catch (Exception e)
        {
            e.printStackTrace();
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
}
