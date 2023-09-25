package thut.perms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;
import thut.perms.config.Config.ConfigData;
import thut.perms.config.Configure;
import thut.perms.management.GroupManager;

public class Config extends ConfigData
{
    @Configure(category = "general")
    public boolean clientEnabled = true;
    @Configure(category = "general")
    public boolean debug = false;
    @Configure(category = "general")
    public boolean disabled = false;
    @Configure(category = "general")
    public boolean argument_perms = false;

    @Configure(category = "general")
    public List<String> command_renames = Lists.newArrayList();

    @Configure(category = "general")
    public List<String> command_alternates = Lists.newArrayList();

    @Configure(category = "general")
    public String lang_file = "en_us.json";

    private final Path configpath;

    public Config()
    {
        super(Perms.MODID);
        this.configpath = FMLPaths.CONFIGDIR.get().resolve(Perms.MODID);
    }

    private final Map<String, String> command_map = Maps.newHashMap();

    private final Map<String, String> lang_overrides_map = Maps.newHashMap();

    private final Map<String, List<String>> command_alternates_map = Maps.newHashMap();

    public void sendFeedback(final CommandSourceStack target, final String key, final boolean log, final Object... args)
    {
        if (this.lang_overrides_map.containsKey(key))
            target.sendSuccess(() -> Component.literal(String.format(this.lang_overrides_map.get(key), args)), log);
        else target.sendSuccess(() -> Component.translatable(key, args), log);
    }

    public void sendError(final CommandSourceStack target, final String key, final Object... args)
    {
        if (this.lang_overrides_map.containsKey(key))
            target.sendFailure(Component.literal(String.format(this.lang_overrides_map.get(key), args)));
        else target.sendFailure(Component.translatable(key, args));
    }

    @Override
    public void onUpdated()
    {
        if (GroupManager.get_instance() != null && Perms.getServer() != null) Perms.loadPerms();

        for (final String s : this.command_renames)
        {
            final String[] args = s.split(":");
            if (args.length != 2) Perms.LOGGER.warn("Invalid command rename: {}, it must be of form \"<old>:<new>\"");
            else this.command_map.put(args[0], args[1]);
        }

        final File file = this.configpath.resolve(this.lang_file).toFile();
        if (file.exists()) try
        {
            final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
            final Gson gson = new GsonBuilder().create();
            final JsonObject o = gson.fromJson(in, JsonObject.class);
            for (final Entry<String, JsonElement> entry : o.entrySet()) try
            {
                final String key = entry.getKey();
                final String value = entry.getValue().getAsString();
                this.lang_overrides_map.put(key, value);
            }
            catch (final Exception e)
            {
                Perms.LOGGER.error("Error with keypair {}, {}", entry.getKey(), entry.getValue());
            }
        }
        catch (final Exception e)
        {
            Perms.LOGGER.error("Error loading lang json from config!", e);
        }
        for (final String s : this.command_alternates)
        {
            final String[] args = s.split(":");
            if (args.length < 2) Perms.LOGGER
                    .warn("Invalid command alternates: {}, it must be of form \"<old>:<alternate1>:<alternate2>:etc\"");
            else
            {
                final List<String> alts = Lists.newArrayList();
                for (int i = 1; i < args.length; i++) alts.add(args[i]);
                this.command_alternates_map.put(args[0], alts);
            }
        }
    }

    public String getRename(final String old)
    {
        if (this.command_map.containsKey(old)) return this.command_map.get(old);
        else return old;
    }

    public List<String> getAlternates(final String old)
    {
        if (this.command_alternates_map.containsKey(old)) return this.command_alternates_map.get(old);
        else return Collections.emptyList();
    }
}
