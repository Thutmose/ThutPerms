package thut.perms.commands;

import java.util.Collection;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.perms.Perms;
import thut.perms.management.Group;

public class EditGroup
{
    private static SuggestionProvider<CommandSource> ADD    = (ctx, sb) -> net.minecraft.command.ISuggestionProvider
            .suggest(Lists.newArrayList("add"), sb);
    private static SuggestionProvider<CommandSource> REMOVE = (ctx, sb) -> net.minecraft.command.ISuggestionProvider
            .suggest(Lists.newArrayList("remove"), sb);

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "edit_group";
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP,
                "Can the player edit a permissions group.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));

        // Set up the command's arguments
        command = command.then(Commands.argument("group", StringArgumentType.string())).then(Commands.argument("add",
                StringArgumentType.string()).suggests(EditGroup.ADD).then(Commands.argument("player",
                        GameProfileArgument.gameProfile()).executes(ctx -> EditGroup.executeAdd(ctx.getSource(),
                                StringArgumentType.getString(ctx, "group"), GameProfileArgument.getGameProfiles(ctx,
                                        "player")))));

        // Actually register the command.
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));

        // Set up the command's arguments
        command = command.then(Commands.argument("group", StringArgumentType.string())).then(Commands.argument("remove",
                StringArgumentType.string()).suggests(EditGroup.REMOVE).then(Commands.argument("player",
                        GameProfileArgument.gameProfile()).executes(ctx -> EditGroup.executeRemove(ctx.getSource(),
                                StringArgumentType.getString(ctx, "group"), GameProfileArgument.getGameProfiles(ctx,
                                        "player")))));

        // Actually register the command.
        commandDispatcher.register(command);
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
                g.members.add(profile.getId());
                Perms.savePerms();
                String name = profile.getName();
                if (name == null) name = profile.getId().toString();
                Perms.config.sendFeedback(source, "thutperms.group.add", true, groupName, name);
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
                g.members.remove(profile.getId());
                Perms.savePerms();
                String name = profile.getName();
                if (name == null) name = profile.getId().toString();
                Perms.config.sendFeedback(source, "thutperms.group.add", true, groupName, name);
            }
            catch (final IllegalArgumentException e)
            {
                Perms.config.sendError(source, e.getMessage());
            }
        return 0;
    }
}
