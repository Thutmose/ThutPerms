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

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.perms.Perms;
import thut.perms.management.Group;
import thut.perms.management.GroupManager;
import thut.perms.management.PermNodes;
import thut.perms.management.PermNodes.DefaultPermissionLevel;

public class EditGroup
{
    public static SuggestionProvider<CommandSourceStack> groupSuggest()
    {
        return (ctx, sb) -> {
            final java.util.List<String> names = Lists.newArrayList();
            for (final Group g : Lists.newArrayList(GroupManager.get_instance().groups)) names.add(g.name);
            names.add(GroupManager.get_instance().initial.name);
            names.add(GroupManager.get_instance().mods.name);
            Collections.sort(names);
            return net.minecraft.commands.SharedSuggestionProvider.suggest(names, sb);
        };
    }

    public static SuggestionProvider<CommandSourceStack> permsSuggest()
    {
        return (ctx, sb) -> {
            final java.util.List<String> names = Lists.newArrayList();
            PermissionAPI.getRegisteredNodes().forEach(n -> names.add(n.getNodeName()));
            Collections.sort(names);
            return net.minecraft.commands.SharedSuggestionProvider.suggest(names, sb);
        };
    }

    public static boolean valid(String perm)
    {
        final java.util.List<String> perms = Lists.newArrayList();
        PermissionAPI.getRegisteredNodes().forEach(n -> perms.add(n.getNodeName()));
        if (perms.contains(perm)) return true;
        if (perm.endsWith("*"))
        {
            perm = perm.substring(0, perm.length() - 1);
            for (final String s : perms) if (s.startsWith(perm)) return true;
        }
        return false;
    }

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "edit_group";
        String perm;
        PermNodes.registerNode(perm = "command." + name, DefaultPermissionLevel.OP,
                "Can the player edit a permissions group.");

        final SuggestionProvider<CommandSourceStack> GROUPS = EditGroup.groupSuggest();

        // Setup with name and permission @formatter:off
        final LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name)
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

    private static ArgumentBuilder<CommandSourceStack, ?> add(final CommandDispatcher<CommandSourceStack> dispatcher)
    {
        String perm;
        PermNodes.registerNode(perm = "command.edit_group.add", DefaultPermissionLevel.OP,
                "Can the player add players to a group.");

        final Command<CommandSourceStack> cmd = ctx -> EditGroup.executeAddGroup(ctx.getSource(),
                StringArgumentType.getString(ctx, "group"), GameProfileArgument.getGameProfiles(ctx, "player"));

        return Commands.literal("add").requires(cs -> CommandManager.hasPerm(cs, perm))
                .then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(cmd));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> remove(final CommandDispatcher<CommandSourceStack> dispatcher)
    {
        String perm;
        PermNodes.registerNode(perm = "command.edit_group.remove", DefaultPermissionLevel.OP,
                "Can the player remove players from a group.");

        final Command<CommandSourceStack> cmd = ctx -> EditGroup.executeRemoveGroup(ctx.getSource(),
                StringArgumentType.getString(ctx, "group"), GameProfileArgument.getGameProfiles(ctx, "player"));

        return Commands.literal("remove").requires(cs -> CommandManager.hasPerm(cs, perm))
                .then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(cmd));
    }

    private static int executeAddGroup(final CommandSourceStack source, final String groupName,
            final Collection<GameProfile> players)
    {
        final Group g = Perms.getGroup(groupName);
        if (g == null)
        {
            Perms.config.sendError(source, "thutperms.nogroup");
            return 1;
        }
        for (final GameProfile profile : players) try
        {
            String name = profile.getName();
            if (name == null) name = profile.getId().toString();
            if (g.members.add(profile.getId()))
            {
                // This removes the group id mapping from other group, and
                // updates the UUID mappings for lookups
                Perms.addToGroup(profile.getId(), groupName);

                Perms.savePerms();
                Perms.config.sendFeedback(source, "thutperms.group.added", true, name, groupName);
                final ServerPlayer player = source.getServer().getPlayerList().getPlayer(profile.getId());
                if (player != null) source.getServer().getCommands().sendCommands(player);
            }
            else Perms.config.sendFeedback(source, "thutperms.group.already_in", true, name, groupName);
        }
        catch (final IllegalArgumentException e)
        {
            Perms.config.sendError(source, e.getMessage());
        }
        return 0;
    }

    private static int executeRemoveGroup(final CommandSourceStack source, final String groupName,
            final Collection<GameProfile> players)
    {
        final Group g = Perms.getGroup(groupName);
        if (g == null)
        {
            Perms.config.sendError(source, "thutperms.nogroup");
            return 1;
        }
        for (final GameProfile profile : players) try
        {
            String name = profile.getName();
            if (name == null) name = profile.getId().toString();
            if (g.members.remove(profile.getId()))
            {
                GroupManager.get_instance()._groupIDMap.remove(profile.getId());
                Perms.savePerms();
                Perms.config.sendFeedback(source, "thutperms.group.removed", true, name, groupName);
                final ServerPlayer player = source.getServer().getPlayerList().getPlayer(profile.getId());
                if (player != null) source.getServer().getCommands().sendCommands(player);
            }
            else Perms.config.sendFeedback(source, "thutperms.group.not_in", true, name, groupName);
        }
        catch (final IllegalArgumentException e)
        {
            Perms.config.sendError(source, e.getMessage());
        }
        return 0;
    }

    private static int executeAddPerm(final CommandSourceStack source, final String perm, final String groupName)
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

    private static int executeRemovePerm(final CommandSourceStack source, final String perm, final String groupName)
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

    private static int executeDenyPerm(final CommandSourceStack source, final String perm, final String groupName)
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

    private static int executeUnDenyPerm(final CommandSourceStack source, final String perm, final String groupName)
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

    private static ArgumentBuilder<CommandSourceStack, ?> add_perm(
            final CommandDispatcher<CommandSourceStack> dispatcher)
    {
        String perm;
        PermNodes.registerNode(perm = "command.edit_player.add", DefaultPermissionLevel.OP,
                "Can the player add perms for another player.");
        final Command<CommandSourceStack> cmd = ctx -> EditGroup.executeAddPerm(ctx.getSource(),
                StringArgumentType.getString(ctx, "perm"), StringArgumentType.getString(ctx, "group"));

        return Commands.literal("add_perm").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands
                .argument("perm", StringArgumentType.greedyString()).suggests(EditGroup.permsSuggest()).executes(cmd));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> remove_perm(
            final CommandDispatcher<CommandSourceStack> dispatcher)
    {
        String perm;
        PermNodes.registerNode(perm = "command.edit_player.remove", DefaultPermissionLevel.OP,
                "Can the player remove perms for another player.");
        final Command<CommandSourceStack> cmd = ctx -> EditGroup.executeRemovePerm(ctx.getSource(),
                StringArgumentType.getString(ctx, "perm"), StringArgumentType.getString(ctx, "group"));

        return Commands.literal("remove_perm").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands
                .argument("perm", StringArgumentType.greedyString()).suggests(EditGroup.permsSuggest()).executes(cmd));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> deny_perm(
            final CommandDispatcher<CommandSourceStack> dispatcher)
    {
        String perm;
        PermNodes.registerNode(perm = "command.edit_player.deny", DefaultPermissionLevel.OP,
                "Can the player deny perms for another player.");
        final Command<CommandSourceStack> cmd = ctx -> EditGroup.executeDenyPerm(ctx.getSource(),
                StringArgumentType.getString(ctx, "perm"), StringArgumentType.getString(ctx, "group"));

        return Commands.literal("deny_perm").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands
                .argument("perm", StringArgumentType.greedyString()).suggests(EditGroup.permsSuggest()).executes(cmd));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> un_deny(
            final CommandDispatcher<CommandSourceStack> dispatcher)
    {
        String perm;
        PermNodes.registerNode(perm = "command.edit_player.un_deny", DefaultPermissionLevel.OP,
                "Can the player undeny perms for another player.");
        final Command<CommandSourceStack> cmd = ctx -> EditGroup.executeUnDenyPerm(ctx.getSource(),
                StringArgumentType.getString(ctx, "perm"), StringArgumentType.getString(ctx, "group"));

        return Commands.literal("un_deny_perm").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands
                .argument("perm", StringArgumentType.greedyString()).suggests(EditGroup.permsSuggest()).executes(cmd));
    }
}
