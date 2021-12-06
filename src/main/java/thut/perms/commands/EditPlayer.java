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

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.perms.Perms;
import thut.perms.management.GroupManager;
import thut.perms.management.Player;

public class EditPlayer
{
    final static Map<String, ChatFormatting> charCodeMap = Maps.newHashMap();

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        if (EditPlayer.charCodeMap.isEmpty()) for (final ChatFormatting format : ChatFormatting.values())
            EditPlayer.charCodeMap.put(format.code + "", format);

        final String name = "edit_player";
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP,
                "Can the player edit a player's suffix or prefix.");

        // Setup with name and permission @formatter:off
        final LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name)
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

    private static ArgumentBuilder<CommandSourceStack, ?> prefix(final CommandDispatcher<CommandSourceStack> dispatcher)
    {
        String perm;
        PermissionAPI.registerNode(perm = "command.edit_player.prefix", DefaultPermissionLevel.OP,
                "Can the player add players to a group.");

        final Command<CommandSourceStack> cmd = ctx -> EditPlayer.executePrefix(ctx.getSource(), EditPlayer.format(
                StringArgumentType.getString(ctx, "words")), GameProfileArgument.getGameProfiles(ctx, "player"));

        return Commands.literal("prefix").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument(
                "words", StringArgumentType.greedyString()).executes(cmd));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> suffix(final CommandDispatcher<CommandSourceStack> dispatcher)
    {
        String perm;
        PermissionAPI.registerNode(perm = "command.edit_player.suffix", DefaultPermissionLevel.OP,
                "Can the player remove players from a group.");

        final Command<CommandSourceStack> cmd = ctx -> EditPlayer.executeSuffix(ctx.getSource(), EditPlayer.format(
                StringArgumentType.getString(ctx, "words")), GameProfileArgument.getGameProfiles(ctx, "player"));

        return Commands.literal("suffix").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument(
                "words", StringArgumentType.greedyString()).executes(cmd));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> add_perm(final CommandDispatcher<CommandSourceStack> dispatcher)
    {
        String perm;
        PermissionAPI.registerNode(perm = "command.edit_player.add", DefaultPermissionLevel.OP,
                "Can the player add perms for another player.");
        final Command<CommandSourceStack> cmd = ctx -> EditPlayer.executeAddPerm(ctx.getSource(), StringArgumentType
                .getString(ctx, "perm"), GameProfileArgument.getGameProfiles(ctx, "player"));

        return Commands.literal("add_perm").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument(
                "perm", StringArgumentType.greedyString()).suggests(EditGroup.permsSuggest()).executes(cmd));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> remove_perm(final CommandDispatcher<CommandSourceStack> dispatcher)
    {
        String perm;
        PermissionAPI.registerNode(perm = "command.edit_player.remove", DefaultPermissionLevel.OP,
                "Can the player remove perms for another player.");
        final Command<CommandSourceStack> cmd = ctx -> EditPlayer.executeRemovePerm(ctx.getSource(), StringArgumentType
                .getString(ctx, "perm"), GameProfileArgument.getGameProfiles(ctx, "player"));

        return Commands.literal("remove_perm").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument(
                "perm", StringArgumentType.greedyString()).suggests(EditGroup.permsSuggest()).executes(cmd));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> deny_perm(final CommandDispatcher<CommandSourceStack> dispatcher)
    {
        String perm;
        PermissionAPI.registerNode(perm = "command.edit_player.deny", DefaultPermissionLevel.OP,
                "Can the player deny perms for another player.");
        final Command<CommandSourceStack> cmd = ctx -> EditPlayer.executeDenyPerm(ctx.getSource(), StringArgumentType
                .getString(ctx, "perm"), GameProfileArgument.getGameProfiles(ctx, "player"));

        return Commands.literal("deny_perm").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument(
                "perm", StringArgumentType.greedyString()).suggests(EditGroup.permsSuggest()).executes(cmd));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> un_deny(final CommandDispatcher<CommandSourceStack> dispatcher)
    {
        String perm;
        PermissionAPI.registerNode(perm = "command.edit_player.un_deny", DefaultPermissionLevel.OP,
                "Can the player undeny perms for another player.");
        final Command<CommandSourceStack> cmd = ctx -> EditPlayer.executeUnDenyPerm(ctx.getSource(), StringArgumentType
                .getString(ctx, "perm"), GameProfileArgument.getGameProfiles(ctx, "player"));

        return Commands.literal("un_deny_perm").requires(cs -> CommandManager.hasPerm(cs, perm)).then(Commands.argument(
                "perm", StringArgumentType.greedyString()).suggests(EditGroup.permsSuggest()).executes(cmd));
    }

    private static int executePrefix(final CommandSourceStack source, final String prefix,
            final Collection<GameProfile> players)
    {
        final MinecraftServer server = source.getServer();
        for (final GameProfile profile : players)
        {
            final Player p = GroupManager.get_instance()._manager.getPlayer(profile.getId());
            p.prefix = prefix;
            if (prefix.trim().isEmpty()) p.prefix = "";
            GroupManager.get_instance()._manager.savePlayer(profile.getId());
            final ServerPlayer player = server.getPlayerList().getPlayer(profile.getId());
            if (player != null) GroupManager.get_instance().updateName(player);
            Perms.config.sendFeedback(source, "thutperms.prefix.set", true, profile.getName(), prefix);
        }
        return 0;
    }

    private static int executeSuffix(final CommandSourceStack source, final String suffix,
            final Collection<GameProfile> players)
    {
        final MinecraftServer server = source.getServer();
        for (final GameProfile profile : players)
        {
            final Player p = GroupManager.get_instance()._manager.getPlayer(profile.getId());
            p.suffix = suffix;
            if (suffix.trim().isEmpty()) p.suffix = "";
            GroupManager.get_instance()._manager.savePlayer(profile.getId());
            final ServerPlayer player = server.getPlayerList().getPlayer(profile.getId());
            if (player != null) GroupManager.get_instance().updateName(player);
            Perms.config.sendFeedback(source, "thutperms.suffix.set", true, profile.getName(), suffix);
        }
        return 0;
    }

    private static int executeAddPerm(final CommandSourceStack source, final String perm,
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

    private static int executeRemovePerm(final CommandSourceStack source, final String perm,
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

    private static int executeDenyPerm(final CommandSourceStack source, final String perm,
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

    private static int executeUnDenyPerm(final CommandSourceStack source, final String perm,
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
                    final ChatFormatting format = EditPlayer.charCodeMap.get(num);
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
