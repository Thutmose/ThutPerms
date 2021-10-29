package thut.perms.management;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fmllegacy.LogicalSidedProvider;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.context.IContext;
import thut.perms.Perms;
import thut.perms.commands.AddGroup;
import thut.perms.commands.CommandManager;
import thut.perms.commands.EditGroup;
import thut.perms.commands.EditPlayer;
import thut.perms.commands.List;
import thut.perms.commands.Reload;

public class PermissionsManager implements IPermissionHandler
{
    public static final GameProfile                              testProfile          = new GameProfile(new UUID(
            1234567987, 123545787), "_permtest_");
    public static ServerPlayer                                   testPlayer;
    public boolean                                               SPDiabled            = false;
    private static final HashMap<String, DefaultPermissionLevel> PERMISSION_LEVEL_MAP = new HashMap<>();
    private static final HashMap<String, String>                 DESCRIPTION_MAP      = new HashMap<>();
    private boolean                                              checkedPerm          = false;
    private static Field                                         CHILDFIELD           = null;
    private static Field                                         LITERFIELD           = null;
    private static Field                                         ARGFIELD             = null;
    private static Field                                         REQFIELD             = null;
    private String                                               lastPerm             = "";

    static
    {
        PermissionsManager.CHILDFIELD = CommandNode.class.getDeclaredFields()[0];
        PermissionsManager.LITERFIELD = CommandNode.class.getDeclaredFields()[1];
        PermissionsManager.ARGFIELD = CommandNode.class.getDeclaredFields()[2];
        PermissionsManager.REQFIELD = CommandNode.class.getDeclaredFields()[3];
        PermissionsManager.CHILDFIELD.setAccessible(true);
        PermissionsManager.LITERFIELD.setAccessible(true);
        PermissionsManager.ARGFIELD.setAccessible(true);
        PermissionsManager.REQFIELD.setAccessible(true);
    }

    public void set(final IPermissionHandler permissionHandler)
    {
        for (final String node : permissionHandler.getRegisteredNodes())
        {
            final DefaultPermissionLevel level = permissionHandler.hasPermission(PermissionsManager.testProfile, node,
                    null) ? DefaultPermissionLevel.ALL : DefaultPermissionLevel.OP;
            this.registerNode(node, level, permissionHandler.getNodeDescription(node));
            Perms.LOGGER.info("Copied values for node " + node);
        }
    }

    @Override
    public void registerNode(final String node, final DefaultPermissionLevel level, final String desc)
    {
        PermissionsManager.PERMISSION_LEVEL_MAP.put(node, level);
        if (!desc.isEmpty()) PermissionsManager.DESCRIPTION_MAP.put(node, desc);
    }

    @Override
    public Collection<String> getRegisteredNodes()
    {
        return Collections.unmodifiableSet(PermissionsManager.PERMISSION_LEVEL_MAP.keySet());
    }

    @Override
    public boolean hasPermission(final GameProfile profile, final String node, @Nullable final IContext context)
    {
        this.checkedPerm = true;
        this.lastPerm = node;
        final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        if (this.SPDiabled && server.isSingleplayer()) return true;
        if (GroupManager.get_instance() == null)
        {
            Perms.LOGGER.warn(node + " is being checked before load!");
            Thread.dumpStack();
            return this.getDefaultPermissionLevel(node) == DefaultPermissionLevel.ALL;
        }
        final boolean value = GroupManager.get_instance().hasPermission(profile.getId(), node);
        if (Perms.config.debug) Perms.LOGGER.info("permnode: " + node + " " + profile + " " + value);
        return value;
    }

    @Override
    public String getNodeDescription(final String node)
    {
        final String desc = PermissionsManager.DESCRIPTION_MAP.get(node);
        return desc == null ? "" : desc;
    }

    /**
     * @return The default permission level of a node. If the permission isn't
     *         registred, it will return NONE
     */
    public DefaultPermissionLevel getDefaultPermissionLevel(final String node)
    {
        final DefaultPermissionLevel level = PermissionsManager.PERMISSION_LEVEL_MAP.get(node);
        return level == null ? DefaultPermissionLevel.NONE : level;
    }

    public void onRegisterCommands(final RegisterCommandsEvent event)
    {
        AddGroup.register(event.getDispatcher());
        EditGroup.register(event.getDispatcher());
        EditPlayer.register(event.getDispatcher());
        List.register(event.getDispatcher());
        Reload.register(event.getDispatcher());
    }

    public void wrapCommands(final MinecraftServer server)
    {
        this.wrapCommands(server, server.getCommands().getDispatcher());
    }

    private void wrap_children(final CommandNode<CommandSourceStack> node, final String lastNode,
            final DefaultPermissionLevel prevLevel)
    {
        this.checkedPerm = false;
        if (node.getRequirement() != null) node.getRequirement().test(PermissionsManager.testPlayer
                .createCommandSourceStack());
        final String perm = lastNode + "." + node.getName();
        if (!this.checkedPerm)
        {
            this.registerNode(perm, prevLevel, "auto generated perm for argument " + node.getName());
            final Predicate<CommandSourceStack> req = (cs) ->
            {
                return CommandManager.hasPerm(cs, perm);
            };
            try
            {
                PermissionsManager.REQFIELD.set(node, req);
            }
            catch (IllegalArgumentException | IllegalAccessException e)
            {
            }
        }
        for (final CommandNode<CommandSourceStack> child : node.getChildren())
            this.wrap_children(child, perm, prevLevel);
    }

    private final Map<String, String> renames = Maps.newHashMap();

    private void wrap(final CommandNode<CommandSourceStack> node, final CommandNode<CommandSourceStack> parent,
            final CommandSourceStack source)
    {
        if (parent instanceof RootCommandNode<?>)
        {
            this.lastPerm = "command.";
            this.checkedPerm = false;
            boolean all = node.getRequirement() == null;
            if (!all) all = node.getRequirement().test(PermissionsManager.testPlayer.createCommandSourceStack());
            if (!this.lastPerm.endsWith(".")) this.lastPerm = this.lastPerm + ".";
            final String perm = this.lastPerm + node.getName();
            final DefaultPermissionLevel level = all ? DefaultPermissionLevel.ALL : DefaultPermissionLevel.OP;
            if (node instanceof LiteralCommandNode<?>)
            {
                final LiteralCommandNode<?> literal = (LiteralCommandNode<?>) node;
                final String rename = Perms.config.getRename(literal.getLiteral());
                if (!rename.equals(literal.getLiteral())) try
                {
                    final Field f = literal.getClass().getDeclaredFields()[0];
                    f.setAccessible(true);
                    if (Perms.config.debug) Perms.LOGGER.info("Renaming {} to {}", literal.getLiteral(), rename);
                    this.renames.put(literal.getLiteral(), rename);
                    f.set(literal, rename);
                }
                catch (final Exception e)
                {
                    Perms.LOGGER.error("Error setting field!", e);
                }
            }

            if (!this.checkedPerm)
            {
                this.registerNode(perm, level, "auto generated perm for command /" + node.getName());
                final Predicate<CommandSourceStack> req = (cs) ->
                {
                    return CommandManager.hasPerm(cs, perm);
                };
                try
                {
                    PermissionsManager.REQFIELD.set(node, req);
                }
                catch (IllegalArgumentException | IllegalAccessException e)
                {
                    Perms.LOGGER.error("Error setting field!", e);
                }
            }
            if (Perms.config.argument_perms) for (final CommandNode<CommandSourceStack> child : node.getChildren())
                this.wrap_children(child, perm, level);
            return;
        }

        for (final CommandNode<CommandSourceStack> child : node.getChildren())
            this.wrap(child, node, source);
    }

    private void wrapCommands(final MinecraftServer server,
            final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        // shouldn't be null, but might be if something goes funny connecting to
        // servers.
        if (server == null) return;
        PermissionsManager.testPlayer = FakePlayerFactory.get(server.getLevel(Level.OVERWORLD),
                PermissionsManager.testProfile);

        this.renames.clear();
        final CommandNode<CommandSourceStack> root = commandDispatcher.getRoot();
        // First wrap and rename the commands.
        this.wrap(root, null, server.createCommandSourceStack());

        // Process the renamed commands.
        this.renames.forEach((o, n) ->
        {
            try
            {
                @SuppressWarnings("unchecked")
                final Map<String, CommandNode<CommandSourceStack>> children = (Map<String, CommandNode<CommandSourceStack>>) PermissionsManager.CHILDFIELD
                        .get(root);
                @SuppressWarnings("unchecked")
                final Map<String, LiteralCommandNode<CommandSourceStack>> literals = (Map<String, LiteralCommandNode<CommandSourceStack>>) PermissionsManager.LITERFIELD
                        .get(root);
                final CommandNode<CommandSourceStack> child = children.remove(o);
                if (child != null) children.put(n, child);
                final LiteralCommandNode<CommandSourceStack> literal = literals.remove(o);
                if (literal != null) literals.put(n, literal);
            }
            catch (final Exception e)
            {
                Perms.LOGGER.error("Error getting field!", e);
            }
        });

        final Map<CommandNode<CommandSourceStack>, java.util.List<LiteralArgumentBuilder<CommandSourceStack>>> newNodes = Maps
                .newHashMap();

        // Then assign alternates.
        for (final CommandNode<CommandSourceStack> node : root.getChildren())
            if (node instanceof LiteralCommandNode<?>)
            {
                final LiteralCommandNode<?> literal = (LiteralCommandNode<?>) node;
                final java.util.List<LiteralArgumentBuilder<CommandSourceStack>> builders = Lists.newArrayList();
                for (final String s : Perms.config.getAlternates(literal.getLiteral()))
                {
                    if (Perms.config.debug) Perms.LOGGER.info("Generating Alternate {} for {}", s, literal
                            .getLiteral());
                    builders.add(Commands.literal(s).requires(node.getRequirement()).redirect(node));
                }
                if (!builders.isEmpty()) newNodes.put(node, builders);
            }
        newNodes.forEach((node, list) ->
        {
            for (final LiteralArgumentBuilder<CommandSourceStack> builder : list)
                commandDispatcher.register(builder);
        });
    }
}
