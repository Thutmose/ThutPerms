package thut.perms.commands;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.perms.Perms;
import thut.perms.management.GroupManager;
import thut.perms.management.Player;

public class EditPlayer
{
    final static Map<String, TextFormatting> charCodeMap = Maps.newHashMap();

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        if (EditPlayer.charCodeMap.isEmpty()) for (final TextFormatting format : TextFormatting.values())
            EditPlayer.charCodeMap.put(format.formattingCode + "", format);

        final String name = "edit_player";
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP,
                "Can the player edit a player's suffix or prefix.");

        // Setup with name and permission @formatter:off
        final LiteralArgumentBuilder<CommandSource> command = Commands.literal(name)
                .requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument("player",
                        GameProfileArgument.gameProfile())
                .then(EditPlayer.prefix(commandDispatcher))
                .then(EditPlayer.suffix(commandDispatcher))
                .then(EditPlayer.add_perm(commandDispatcher))
                .then(EditPlayer.remove_perm(commandDispatcher))
                .then(EditPlayer.deny_perm(commandDispatcher))
                .then(EditPlayer.un_deny(commandDispatcher)));
        //@formatter:on
        commandDispatcher.register(command);
    }

    private static ArgumentBuilder<CommandSource, ?> prefix(final CommandDispatcher<CommandSource> dispatcher)
    {
        String perm;
        PermissionAPI.registerNode(perm = "command.edit_player.prefix", DefaultPermissionLevel.OP,
                "Can the player add players to a group.");

        final Command<CommandSource> cmd = ctx -> EditPlayer.executePrefix(ctx.getSource(), EditPlayer.format(
                StringArgumentType.getString(ctx, "words")), GameProfileArgument.getGameProfiles(ctx, "player"));

        return Commands.literal("prefix").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument(
                "words", StringArgumentType.greedyString()).executes(cmd));
    }

    private static ArgumentBuilder<CommandSource, ?> suffix(final CommandDispatcher<CommandSource> dispatcher)
    {
        String perm;
        PermissionAPI.registerNode(perm = "command.edit_player.suffix", DefaultPermissionLevel.OP,
                "Can the player remove players from a group.");

        final Command<CommandSource> cmd = ctx -> EditPlayer.executeSuffix(ctx.getSource(), EditPlayer.format(
                StringArgumentType.getString(ctx, "words")), GameProfileArgument.getGameProfiles(ctx, "player"));

        return Commands.literal("suffix").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument(
                "words", StringArgumentType.greedyString()).executes(cmd));
    }

    private static ArgumentBuilder<CommandSource, ?> add_perm(final CommandDispatcher<CommandSource> dispatcher)
    {
        String perm;
        PermissionAPI.registerNode(perm = "command.edit_player.add", DefaultPermissionLevel.OP,
                "Can the player add perms for another player.");
        final Command<CommandSource> cmd = ctx -> EditPlayer.executeAddPerm(ctx.getSource(), StringArgumentType
                .getString(ctx, "perm"), GameProfileArgument.getGameProfiles(ctx, "player"));

        return Commands.literal("add_perm").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument(
                "perm", StringArgumentType.greedyString()).suggests(EditGroup.permsSuggest()).executes(cmd));
    }

    private static ArgumentBuilder<CommandSource, ?> remove_perm(final CommandDispatcher<CommandSource> dispatcher)
    {
        String perm;
        PermissionAPI.registerNode(perm = "command.edit_player.remove", DefaultPermissionLevel.OP,
                "Can the player remove perms for another player.");
        final Command<CommandSource> cmd = ctx -> EditPlayer.executeRemovePerm(ctx.getSource(), StringArgumentType
                .getString(ctx, "perm"), GameProfileArgument.getGameProfiles(ctx, "player"));

        return Commands.literal("remove_perm").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument(
                "perm", StringArgumentType.greedyString()).suggests(EditGroup.permsSuggest()).executes(cmd));
    }

    private static ArgumentBuilder<CommandSource, ?> deny_perm(final CommandDispatcher<CommandSource> dispatcher)
    {
        String perm;
        PermissionAPI.registerNode(perm = "command.edit_player.deny", DefaultPermissionLevel.OP,
                "Can the player deny perms for another player.");
        final Command<CommandSource> cmd = ctx -> EditPlayer.executeDenyPerm(ctx.getSource(), StringArgumentType
                .getString(ctx, "perm"), GameProfileArgument.getGameProfiles(ctx, "player"));

        return Commands.literal("deny_perm").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument(
                "perm", StringArgumentType.greedyString()).suggests(EditGroup.permsSuggest()).executes(cmd));
    }

    private static ArgumentBuilder<CommandSource, ?> un_deny(final CommandDispatcher<CommandSource> dispatcher)
    {
        String perm;
        PermissionAPI.registerNode(perm = "command.edit_player.un_deny", DefaultPermissionLevel.OP,
                "Can the player undeny perms for another player.");
        final Command<CommandSource> cmd = ctx -> EditPlayer.executeUnDenyPerm(ctx.getSource(), StringArgumentType
                .getString(ctx, "perm"), GameProfileArgument.getGameProfiles(ctx, "player"));

        return Commands.literal("un_deny_perm").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument(
                "perm", StringArgumentType.greedyString()).suggests(EditGroup.permsSuggest()).executes(cmd));
    }

    private static int executePrefix(final CommandSource source, final String prefix,
            final Collection<GameProfile> players)
    {
        final MinecraftServer server = source.getServer();
        for (final GameProfile profile : players)
        {
            final Player p = GroupManager.get_instance()._manager.getPlayer(profile.getId());
            p.prefix = prefix;
            if (prefix.trim().isEmpty()) p.prefix = "";
            GroupManager.get_instance()._manager.savePlayer(profile.getId());
            final ServerPlayerEntity player = server.getPlayerList().getPlayerByUUID(profile.getId());
            if (player != null) GroupManager.get_instance().updateName(player);
            Perms.config.sendFeedback(source, "thutperms.prefix.set", true, profile.getName(), prefix);
        }
        return 0;
    }

    private static int executeSuffix(final CommandSource source, final String suffix,
            final Collection<GameProfile> players)
    {
        final MinecraftServer server = source.getServer();
        for (final GameProfile profile : players)
        {
            final Player p = GroupManager.get_instance()._manager.getPlayer(profile.getId());
            p.suffix = suffix;
            if (suffix.trim().isEmpty()) p.suffix = "";
            GroupManager.get_instance()._manager.savePlayer(profile.getId());
            final ServerPlayerEntity player = server.getPlayerList().getPlayerByUUID(profile.getId());
            if (player != null) GroupManager.get_instance().updateName(player);
            Perms.config.sendFeedback(source, "thutperms.suffix.set", true, profile.getName(), suffix);
        }
        return 0;
    }

    private static int executeAddPerm(final CommandSource source, final String perm,
            final Collection<GameProfile> players)
    {
        if (!EditGroup.valid(perm)) Perms.config.sendError(source, "thutperms.perm.unknownperm", perm);
        for (final GameProfile profile : players)
        {
            final Player p = GroupManager.get_instance()._manager.getPlayer(profile.getId());
            p.allowedCommands.add(perm);
            p.onUpdated(source.getServer());
            Perms.config.sendFeedback(source, "thutperms.perm.added", true, profile.getName(), perm);
        }
        return 0;
    }

    private static int executeRemovePerm(final CommandSource source, final String perm,
            final Collection<GameProfile> players)
    {
        if (!EditGroup.valid(perm)) Perms.config.sendError(source, "thutperms.perm.unknownperm", perm);
        for (final GameProfile profile : players)
        {
            final Player p = GroupManager.get_instance()._manager.getPlayer(profile.getId());
            p.allowedCommands.remove(perm);
            p.onUpdated(source.getServer());
            Perms.config.sendFeedback(source, "thutperms.perm.removed", true, profile.getName(), perm);
        }
        return 0;
    }

    private static int executeDenyPerm(final CommandSource source, final String perm,
            final Collection<GameProfile> players)
    {
        if (!EditGroup.valid(perm)) Perms.config.sendError(source, "thutperms.perm.unknownperm", perm);
        for (final GameProfile profile : players)
        {
            final Player p = GroupManager.get_instance()._manager.getPlayer(profile.getId());
            p.bannedCommands.add(perm);
            p.onUpdated(source.getServer());
            Perms.config.sendFeedback(source, "thutperms.perm.denied", true, profile.getName(), perm);
        }
        return 0;
    }

    private static int executeUnDenyPerm(final CommandSource source, final String perm,
            final Collection<GameProfile> players)
    {
        if (!EditGroup.valid(perm)) Perms.config.sendError(source, "thutperms.perm.unknownperm", perm);
        for (final GameProfile profile : players)
        {
            final Player p = GroupManager.get_instance()._manager.getPlayer(profile.getId());
            p.bannedCommands.remove(perm);
            p.onUpdated(source.getServer());
            Perms.config.sendFeedback(source, "thutperms.perm.undenied", true, profile.getName(), perm);
        }
        return 0;
    }

    private static String format(String input)
    {
        boolean done = false;

        if (input.startsWith("\"")) input = input.replaceFirst("\"", "");
        if (input.endsWith("\"")) input = input.substring(0, input.length() - 1);

        int index = 0;
        index = input.indexOf('&', index);

        while (!done && index < input.length() && index >= 0)
            try
            {
                done = !input.contains("&");
                index = input.indexOf('&', index);
                if (index < input.length() - 1 && index >= 0)
                {
                    if (index > 0 && input.substring(index - 1, index).equals("\\"))
                    {
                        index++;
                        continue;
                    }
                    final String toReplace = input.substring(index, index + 2);
                    final String num = toReplace.replace("&", "");
                    final TextFormatting format = EditPlayer.charCodeMap.get(num);
                    if (format != null) input = input.replaceAll(toReplace, format + "");
                    else index++;
                }
                else done = true;
            }
            catch (final Exception e)
            {
                done = true;
                e.printStackTrace();
            }

        return input;
    }
}
