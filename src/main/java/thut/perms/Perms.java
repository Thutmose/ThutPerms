package thut.perms;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.FileAppender;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.perms.management.Group;
import thut.perms.management.GroupManager;
import thut.perms.management.PermissionsManager;
import thut.perms.management.PlayerManager;

@Mod(Perms.MODID)
public class Perms
{
    public static final String MODID           = "thutperms";
    public static final Config config          = new Config();
    public static final Logger LOGGER          = LogManager.getLogger(Perms.MODID);
    public static File         jsonFile_groups = null;
    public static File         folder_players  = null;

    public static ExclusionStrategy exclusion = new ExclusionStrategy()
    {
        @Override
        public boolean shouldSkipField(final FieldAttributes f)
        {
            final String name = f.getName();
            return name.startsWith("_");
        }

        @Override
        public boolean shouldSkipClass(final Class<?> clazz)
        {
            if (clazz.getName().contains("net.minecraft")) return true;
            return false;
        }
    };

    public static final PermissionsManager manager    = new PermissionsManager();
    private IPermissionHandler             oldHandler = null;

    public Perms()
    {
        final Path dir = FMLPaths.CONFIGDIR.get().resolve(Perms.MODID);
        Perms.folder_players = dir.resolve("players").toFile();
        Perms.folder_players.mkdirs();
        Perms.jsonFile_groups = dir.resolve("thutperms.json").toFile();

        final File logfile = FMLPaths.GAMEDIR.get().resolve("logs").resolve(Perms.MODID + ".log").toFile();
        if (logfile.exists()) logfile.delete();
        final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) Perms.LOGGER;
        final FileAppender appender = FileAppender.newBuilder().withFileName(logfile.getAbsolutePath()).setName(
                Perms.MODID).build();
        logger.addAppender(appender);
        appender.start();

        thut.essentials.config.Config.setupConfigs(Perms.config, Perms.MODID, Perms.MODID);

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void serverAboutToStart(final FMLServerAboutToStartEvent event)
    {
        if (Perms.config.disabled)
        {
            if (this.oldHandler != null) PermissionAPI.setPermissionHandler(this.oldHandler);
        }
        else if (PermissionAPI.getPermissionHandler() != Perms.manager)
        {
            Perms.manager.set(this.oldHandler = PermissionAPI.getPermissionHandler());
            PermissionAPI.setPermissionHandler(Perms.manager);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void serverStarting(final FMLServerStartingEvent event)
    {
        if (Perms.config.disabled) return;
        Perms.loadPerms();
        GroupManager.get_instance()._server = event.getServer();
        Perms.manager.onServerStarting(event);
    }

    @SubscribeEvent
    public void serverUnload(final FMLServerStoppingEvent evt)
    {
        Perms.savePerms();
    }

    @SubscribeEvent
    void logIn(final PlayerLoggedInEvent event)
    {
        if (Perms.config.disabled) return;
        final PlayerManager manager = GroupManager.get_instance()._manager;
        if (event.getPlayer() instanceof ServerPlayerEntity) manager.createPlayer(event.getPlayer().getUniqueID());
    }

    @SubscribeEvent
    void logOut(final PlayerLoggedInEvent event)
    {
        if (Perms.config.disabled) return;
        final PlayerManager manager = GroupManager.get_instance()._manager;
        if (event.getPlayer() instanceof ServerPlayerEntity) manager.unloadPlayer(event.getPlayer().getUniqueID());
    }

    public static void loadPerms()
    {
        if (Perms.jsonFile_groups.exists())
        {
            String json = null;
            try
            {
                final Gson gson = new GsonBuilder().addDeserializationExclusionStrategy(Perms.exclusion)
                        .setPrettyPrinting().create();
                json = FileUtils.readFileToString(Perms.jsonFile_groups, "UTF-8");
                GroupManager.set_instance(gson.fromJson(json, GroupManager.class));
                GroupManager.get_instance().init();
                Perms.savePerms();
            }
            catch (final Throwable e)
            {
                Perms.LOGGER.error("Error reading groups {}", json);
                Perms.LOGGER.error(e);
            }
            return;
        }
        else
        {
            GroupManager.set_instance(new GroupManager());
            GroupManager.get_instance().init();
            Perms.savePerms();
        }
    }

    public static void savePerms()
    {
        try
        {
            final Gson gson = new GsonBuilder().addSerializationExclusionStrategy(Perms.exclusion).setPrettyPrinting()
                    .create();
            final Predicate<String> nonnull = t -> t == null || t.isEmpty();
            for (final Group group : GroupManager.get_instance().groups)
            {
                group.allowedCommands.removeIf(nonnull);
                group.bannedCommands.removeIf(nonnull);
                if (!group.allowedCommands.isEmpty()) Collections.sort(group.allowedCommands);
                if (!group.bannedCommands.isEmpty()) Collections.sort(group.bannedCommands);
            }
            FileUtils.writeStringToFile(Perms.jsonFile_groups, gson.toJson(GroupManager.get_instance()), "UTF-8");
        }
        catch (final Exception e)
        {
            Perms.LOGGER.error("Error saving group perms", e);
        }
    }

    public static Group addGroup(final String name)
    {
        final Group ret = new Group(name);
        GroupManager.get_instance()._groupNameMap.put(name, ret);
        GroupManager.get_instance().groups.add(ret);
        return ret;
    }

    public static void addToGroup(final UUID id, final String name)
    {
        final Group group = GroupManager.get_instance()._groupNameMap.get(name);
        // Remove from all other groups first.
        GroupManager.get_instance().initial.members.remove(id);
        GroupManager.get_instance().mods.members.remove(id);
        for (final Group old : GroupManager.get_instance().groups)
            old.members.remove(id);
        if (group != null)
        {
            group.members.add(id);
            GroupManager.get_instance()._groupIDMap.put(id, group);
        }
    }

    public static Group getGroup(final String name)
    {
        if (name.equals(GroupManager.get_instance().initial.name)) return GroupManager.get_instance().initial;
        if (name.equals(GroupManager.get_instance().mods.name)) return GroupManager.get_instance().mods;
        return GroupManager.get_instance()._groupNameMap.get(name);
    }
}