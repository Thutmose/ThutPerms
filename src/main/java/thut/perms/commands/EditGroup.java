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
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.perms.Perms;
import thut.perms.management.Group;
import thut.perms.management.GroupManager;

public class EditGroup
{
    public static SuggestionProvider<CommandSource> groupSuggest()
    {
        return (ctx, sb) ->
        {
            final java.util.List<String> names = Lists.newArrayList();
            for (final Group g : Lists.newArrayList(GroupManager.get_instance().groups))
                names.add(g.name);
            names.add(GroupManager.get_instance().initial.name);
            names.add(GroupManager.get_instance().mods.name);
            Collections.sort(names);
            return net.minecraft.command.ISuggestionProvider.suggest(names, sb);
        };
    }

    public static SuggestionProvider<CommandSource> permsSuggest()
    {
        return (ctx, sb) ->
        {
            final java.util.List<String> names = Lists.newArrayList(PermissionAPI.getPermissionHandler()
                    .getRegisteredNodes());
            Collections.sort(names);
            return net.minecraft.command.ISuggestionProvider.suggest(names, sb);
        };
    }

    public static boolean valid(String perm)
    {
        final java.util.List<String> perms = Lists.newArrayList(PermissionAPI.getPermissionHandler()
                .getRegisteredNodes());
        if (perms.contains(perm)) return true;
        if (perm.endsWith("*"))
        {
            perm = perm.substring(0, perm.length() - 1);
            for (final String s : perms)
                if (s.startsWith(perm)) return true;
        }
        return false;
    }

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "edit_group";
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP,
                "Can the player edit a permissions group.");

        final SuggestionProvider<CommandSource> GROUPS = EditGroup.groupSuggest();

        // Setup with name and permission @formatter:off
        final LiteralArgumentBuilder<CommandSource> command = Commands.literal(name)
                .requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument("group",
                        StringArgumentType.string()).suggests(GROUPS)
                .then(EditGroup.add(commandDispatcher))
                .then(EditGroup.remove(commandDispatcher))
                .then(EditGroup.add_perm(commandDispatcher))
                .then(EditGroup.remove_perm(commandDispatcher))
                .then(EditGroup.deny_perm(commandDispatcher))
                .then(EditGroup.un_deny(commandDispatcher)));
        //@formatter:on
        commandDispatcher.register(command);
    }

    private static ArgumentBuilder<CommandSource, ?> add(final CommandDispatcher<CommandSource> dispatcher)
    {
        String perm;
        PermissionAPI.registerNode(perm = "command.edit_group.add", DefaultPermissionLevel.OP,
                "Can the player add players to a group.");

        final Command<CommandSource> cmd = ctx -> EditGroup.executeAddGroup(ctx.getSource(), StringArgumentType
                .getString(ctx, "group"), GameProfileArgument.getGameProfiles(ctx, "player"));

        return Commands.literal("add").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument("player",
                GameProfileArgument.gameProfile()).executes(cmd));
    }

    private static ArgumentBuilder<CommandSource, ?> remove(final CommandDispatcher<CommandSource> dispatcher)
    {
        String perm;
        PermissionAPI.registerNode(perm = "command.edit_group.remove", DefaultPermissionLevel.OP,
                "Can the player remove players from a group.");

        final Command<CommandSource> cmd = ctx -> EditGroup.executeRemoveGroup(ctx.getSource(), StringArgumentType
                .getString(ctx, "group"), GameProfileArgument.getGameProfiles(ctx, "player"));

        return Commands.literal("remove").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument(
                "player", GameProfileArgument.gameProfile()).executes(cmd));
    }

    private static int executeAddGroup(final CommandSource source, final String groupName,
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
                    final ServerPlayerEntity player = source.getServer().getPlayerList().getPlayerByUUID(profile
                            .getId());
                    if (player != null) source.getServer().getCommandManager().send(player);
                }
                else Perms.config.sendFeedback(source, "thutperms.group.already_in", true, name, groupName);
            }
            catch (final IllegalArgumentException e)
            {
                Perms.config.sendError(source, e.getMessage());
            }
        return 0;
    }

    private static int executeRemoveGroup(final CommandSource source, final String groupName,
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
                    final ServerPlayerEntity player = source.getServer().getPlayerList().getPlayerByUUID(profile
                            .getId());
                    if (player != null) source.getServer().getCommandManager().send(player);
                }
                else Perms.config.sendFeedback(source, "thutperms.group.not_in", true, name, groupName);
            }
            catch (final IllegalArgumentException e)
            {
                Perms.config.sendError(source, e.getMessage());
            }
        return 0;
    }

    private static int executeAddPerm(final CommandSource source, final String perm, final String groupName)
    {
        final Group g = Perms.getGroup(groupName);
        if (g == null)
        {
            Perms.config.sendError(source, "thutperms.nogroup");
            return 1;
        }
        if (!EditGroup.valid(perm)) Perms.config.sendError(source, "thutperms.perm.unknownperm", perm);

        g.allowedCommands.add(perm);
        g.onUpdated(source.getServer());
        Perms.savePerms();
        Perms.config.sendFeedback(source, "thutperms.perm.added", true, groupName, perm);
        return 0;
    }

    private static int executeRemovePerm(final CommandSource source, final String perm, final String groupName)
    {
        final Group g = Perms.getGroup(groupName);
        if (g == null)
        {
            Perms.config.sendError(source, "thutperms.nogroup");
            return 1;
        }
        if (!EditGroup.valid(perm)) Perms.config.sendError(source, "thutperms.perm.unknownperm", perm);

        g.allowedCommands.remove(perm);
        g.onUpdated(source.getServer());
        Perms.savePerms();
        Perms.config.sendFeedback(source, "thutperms.perm.removed", true, groupName, perm);
        return 0;
    }

    private static int executeDenyPerm(final CommandSource source, final String perm, final String groupName)
    {
        final Group g = Perms.getGroup(groupName);
        if (g == null)
        {
            Perms.config.sendError(source, "thutperms.nogroup");
            return 1;
        }
        if (!EditGroup.valid(perm)) Perms.config.sendError(source, "thutperms.perm.unknownperm", perm);

        g.bannedCommands.add(perm);
        g.onUpdated(source.getServer());
        Perms.savePerms();
        Perms.config.sendFeedback(source, "thutperms.perm.denied", true, groupName, perm);
        return 0;
    }

    private static int executeUnDenyPerm(final CommandSource source, final String perm, final String groupName)
    {
        final Group g = Perms.getGroup(groupName);
        if (g == null)
        {
            Perms.config.sendError(source, "thutperms.nogroup");
            return 1;
        }
        if (!EditGroup.valid(perm)) Perms.config.sendError(source, "thutperms.perm.unknownperm", perm);

        g.bannedCommands.remove(perm);
        g.onUpdated(source.getServer());
        Perms.savePerms();
        Perms.config.sendFeedback(source, "thutperms.perm.undenied", true, groupName, perm);
        return 0;
    }

    private static ArgumentBuilder<CommandSource, ?> add_perm(final CommandDispatcher<CommandSource> dispatcher)
    {
        String perm;
        PermissionAPI.registerNode(perm = "command.edit_player.add", DefaultPermissionLevel.OP,
                "Can the player add perms for another player.");
        final Command<CommandSource> cmd = ctx -> EditGroup.executeAddPerm(ctx.getSource(), StringArgumentType
                .getString(ctx, "perm"), StringArgumentType.getString(ctx, "group"));

        return Commands.literal("add_perm").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument(
                "perm", StringArgumentType.greedyString()).suggests(EditGroup.permsSuggest()).executes(cmd));
    }

    private static ArgumentBuilder<CommandSource, ?> remove_perm(final CommandDispatcher<CommandSource> dispatcher)
    {
        String perm;
        PermissionAPI.registerNode(perm = "command.edit_player.remove", DefaultPermissionLevel.OP,
                "Can the player remove perms for another player.");
        final Command<CommandSource> cmd = ctx -> EditGroup.executeRemovePerm(ctx.getSource(), StringArgumentType
                .getString(ctx, "perm"), StringArgumentType.getString(ctx, "group"));

        return Commands.literal("remove_perm").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument(
                "perm", StringArgumentType.greedyString()).suggests(EditGroup.permsSuggest()).executes(cmd));
    }

    private static ArgumentBuilder<CommandSource, ?> deny_perm(final CommandDispatcher<CommandSource> dispatcher)
    {
        String perm;
        PermissionAPI.registerNode(perm = "command.edit_player.deny", DefaultPermissionLevel.OP,
                "Can the player deny perms for another player.");
        final Command<CommandSource> cmd = ctx -> EditGroup.executeDenyPerm(ctx.getSource(), StringArgumentType
                .getString(ctx, "perm"), StringArgumentType.getString(ctx, "group"));

        return Commands.literal("deny_perm").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument(
                "perm", StringArgumentType.greedyString()).suggests(EditGroup.permsSuggest()).executes(cmd));
    }

    private static ArgumentBuilder<CommandSource, ?> un_deny(final CommandDispatcher<CommandSource> dispatcher)
    {
        String perm;
        PermissionAPI.registerNode(perm = "command.edit_player.un_deny", DefaultPermissionLevel.OP,
                "Can the player undeny perms for another player.");
        final Command<CommandSource> cmd = ctx -> EditGroup.executeUnDenyPerm(ctx.getSource(), StringArgumentType
                .getString(ctx, "perm"), StringArgumentType.getString(ctx, "group"));

        return Commands.literal("un_deny_perm").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument(
                "perm", StringArgumentType.greedyString()).suggests(EditGroup.permsSuggest()).executes(cmd));
    }
}
