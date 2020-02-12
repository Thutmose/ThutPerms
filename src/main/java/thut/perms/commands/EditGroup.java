package thut.perms.commands;

import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.perms.Perms;
import thut.perms.management.Group;
import thut.perms.management.GroupManager;

public class EditGroup
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "edit_group";
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP,
                "Can the player edit a permissions group.");

        final java.util.List<String> names = Lists.newArrayList();
        for (final Group g : Lists.newArrayList(GroupManager.get_instance().groups))
            names.add(g.name);
        names.add(GroupManager.get_instance().initial.name);
        names.add(GroupManager.get_instance().mods.name);
        Collections.sort(names);

        final SuggestionProvider<CommandSource> GROUPS = (ctx, sb) -> net.minecraft.command.ISuggestionProvider.suggest(
                names, sb);

        // Setup with name and permission @formatter:off
        final LiteralArgumentBuilder<CommandSource> add = Commands.literal(name)
                .requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument("group",
                        StringArgumentType.string()).suggests(GROUPS)
                .then(EditGroup.add(commandDispatcher)));
        //@formatter:on
        commandDispatcher.register(add);

        // Setup with name and permission @formatter:off
        final LiteralArgumentBuilder<CommandSource> remove = Commands.literal(name)
                .requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument("group",
                        StringArgumentType.string()).suggests(GROUPS)
                .then(EditGroup.remove(commandDispatcher)));
        //@formatter:on
        commandDispatcher.register(remove);
    }

    private static ArgumentBuilder<CommandSource, ?> add(final CommandDispatcher<CommandSource> dispatcher)
    {
        String perm;
        PermissionAPI.registerNode(perm = "command.edit_group.add", DefaultPermissionLevel.OP,
                "Can the player add players to a group.");

        final Command<CommandSource> cmd = ctx -> EditGroup.executeAdd(ctx.getSource(), StringArgumentType.getString(
                ctx, "group"), GameProfileArgument.getGameProfiles(ctx, "player"));

        return Commands.literal("add").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument("player",
                GameProfileArgument.gameProfile()).executes(cmd));
    }

    private static ArgumentBuilder<CommandSource, ?> remove(final CommandDispatcher<CommandSource> dispatcher)
    {
        String perm;
        PermissionAPI.registerNode(perm = "command.edit_group.remove", DefaultPermissionLevel.OP,
                "Can the player remove players from a group.");

        final Command<CommandSource> cmd = ctx -> EditGroup.executeRemove(ctx.getSource(), StringArgumentType.getString(
                ctx, "group"), GameProfileArgument.getGameProfiles(ctx, "player"));

        return Commands.literal("remove").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument(
                "player", GameProfileArgument.gameProfile()).executes(cmd));
    }

    private static int executeAdd(final CommandSource source, final String groupName,
            final Collection<GameProfile> players)
    {
        final Group g = Perms.getGroup(groupName);
        if (g == null)
        {
            Perms.config.sendError(source, "thutperms.nogroup");
            return 1;
        }
        for (final GameProfile profile : players)
            try
            {
                String name = profile.getName();
                if (name == null) name = profile.getId().toString();
                if (g.members.add(profile.getId()))
                {
                    Perms.savePerms();
                    Perms.config.sendFeedback(source, "thutperms.group.added", true, name, groupName);
                }
                else Perms.config.sendFeedback(source, "thutperms.group.already_in", true, name, groupName);
            }
            catch (final IllegalArgumentException e)
            {
                Perms.config.sendError(source, e.getMessage());
            }
        return 0;
    }

    private static int executeRemove(final CommandSource source, final String groupName,
            final Collection<GameProfile> players)
    {
        final Group g = Perms.getGroup(groupName);
        if (g == null)
        {
            Perms.config.sendError(source, "thutperms.nogroup");
            return 1;
        }
        for (final GameProfile profile : players)
            try
            {
                String name = profile.getName();
                if (name == null) name = profile.getId().toString();
                if (g.members.remove(profile.getId()))
                {
                    Perms.savePerms();
                    Perms.config.sendFeedback(source, "thutperms.group.removed", true, name, groupName);
                }
                else Perms.config.sendFeedback(source, "thutperms.group.not_in", true, name, groupName);
            }
            catch (final IllegalArgumentException e)
            {
                Perms.config.sendError(source, e.getMessage());
            }
        return 0;
    }
}
