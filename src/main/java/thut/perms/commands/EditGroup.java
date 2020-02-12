package thut.perms.commands;

import java.util.Collection;
import java.util.Collections;

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

        final SuggestionProvider<CommandSource> ADDREM = (ctx, sb) -> net.minecraft.command.ISuggestionProvider.suggest(
                Lists.newArrayList("add", "remove"), sb);

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));

        // Set up the command's arguments
        //@formatter:off
        command = command.then(Commands.argument("group", StringArgumentType.string()).suggests(GROUPS)
                .then(Commands.argument("add", StringArgumentType.string()).suggests(ADDREM)
                    .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .executes(ctx ->
                        StringArgumentType.getString(ctx, "add").equals("add")?
                            EditGroup.executeAdd(ctx.getSource(), StringArgumentType.getString(ctx, "group"),
                                                GameProfileArgument.getGameProfiles(ctx, "player")):
                            EditGroup.executeRemove(ctx.getSource(), StringArgumentType.getString(ctx, "group"),
                                                    GameProfileArgument.getGameProfiles(ctx, "player"))))));
        //@formatter:on
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
