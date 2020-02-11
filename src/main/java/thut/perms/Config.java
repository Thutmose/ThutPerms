package thut.perms;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.command.CommandSource;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import thut.essentials.config.Config.ConfigData;
import thut.essentials.config.Configure;
import thut.perms.management.GroupManager;

public class Config extends ConfigData
{
    @Configure(category = "general")
    public boolean clientEnabled  = true;
    @Configure(category = "general")
    public boolean debug          = false;
    @Configure(category = "general")
    public boolean disabled       = false;
    @Configure(category = "general")
    public boolean argument_perms = false;

    @Configure(category = "general")
    public List<String> command_renames = Lists.newArrayList();

    @Configure(category = "general")
    public List<String> command_alternates = Lists.newArrayList();

    @Configure(category = "general")
    public List<String> lang_overrides = Lists.newArrayList();

    public Config()
    {
        super(Perms.MODID);
    }

    private final Map<String, String> command_map = Maps.newHashMap();

    private final Map<String, String> lang_overrides_map = Maps.newHashMap();

    private final Map<String, List<String>> command_alternates_map = Maps.newHashMap();

    public void sendFeedback(final CommandSource target, final String key, final boolean log, final Object... args)
    {
        if (this.lang_overrides_map.containsKey(key)) target.sendFeedback(new StringTextComponent(String.format(
                this.lang_overrides_map.get(key), args)), log);
        else target.sendFeedback(new TranslationTextComponent(key, args), log);
    }

    public void sendError(final CommandSource target, final String key, final Object... args)
    {
        if (this.lang_overrides_map.containsKey(key)) target.sendErrorMessage(new StringTextComponent(String.format(
                this.lang_overrides_map.get(key), args)));
        else target.sendErrorMessage(new TranslationTextComponent(key, args));
    }

    @Override
    public void onUpdated()
    {
        if (GroupManager.get_instance() != null && GroupManager.get_instance()._server != null) Perms.loadPerms();

        for (final String s : this.command_renames)
        {
            final String[] args = s.split(":");
            if (args.length != 2) Perms.LOGGER.warn("Invalid command rename: {}, it must be of form \"<old>:<new>\"");
            else this.command_map.put(args[0], args[1]);
        }
        for (final String s : this.lang_overrides)
        {
            final String[] args = s.split(":");
            if (args.length < 2) Perms.LOGGER.warn("Invalid lang override: {}, it must be of form \"<key>:<value>\"");
            else
            {
                String value = args[1];
                for (int i = 2; i < args.length; i++)
                    value = value + ":" + args[i];
                this.lang_overrides_map.put(args[0], value);
            }
        }
        for (final String s : this.command_alternates)
        {
            final String[] args = s.split(":");
            if (args.length < 2) Perms.LOGGER.warn(
                    "Invalid command alternates: {}, it must be of form \"<old>:<alternate1>:<alternate2>:etc\"");
            else
            {
                final List<String> alts = Lists.newArrayList();
                for (int i = 1; i < args.length; i++)
                    alts.add(args[i]);
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
