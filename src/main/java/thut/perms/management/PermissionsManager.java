package thut.perms.management;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.server.permission.handler.IPermissionHandler;
import net.minecraftforge.server.permission.nodes.PermissionDynamicContext;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import thut.perms.Perms;
import thut.perms.commands.AddGroup;
import thut.perms.commands.CommandManager;
import thut.perms.commands.EditGroup;
import thut.perms.commands.EditPlayer;
import thut.perms.commands.List;
import thut.perms.commands.Reload;
import thut.perms.management.PermNodes.DefaultPermissionLevel;

public class PermissionsManager implements IPermissionHandler
{
    public boolean SPDiabled = false;
    private boolean checkedPerm = false;
    private static Field CHILDFIELD = null;
    private static Field LITERFIELD = null;
    private static Field ARGFIELD = null;
    private static Field REQFIELD = null;
    private String lastPerm = "";

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

    public static PermissionsManager INSTANCE;

    private static final Map<String, Object> DEFAULTS = Maps.newHashMap();

    private Map<String, PermissionNode<?>> nodeMap = Maps.newHashMap();
    private Set<PermissionNode<?>> nodes;

    public PermissionsManager(Collection<PermissionNode<?>> permissions)
    {
        permissions.forEach(p -> {
            nodeMap.put(p.getNodeName(), p);
            DEFAULTS.put(p.getNodeName(), p.getDefaultResolver().resolve(null, PermNodes.testProfile.getId()));
        });
        nodes = ImmutableSet.copyOf(nodeMap.values());
        INSTANCE = this;
    }

    public static void onRegisterCommands(final RegisterCommandsEvent event)
    {
        AddGroup.register(event.getDispatcher());
        EditGroup.register(event.getDispatcher());
        EditPlayer.register(event.getDispatcher());
        List.register(event.getDispatcher());
        Reload.register(event.getDispatcher());
    }

    public void wrapCommands(final MinecraftServer server)
    {
        // TODO wrapping now...
        this.wrapCommands(server, server.getCommands().getDispatcher());
    }

    private void registerNode(String perm, DefaultPermissionLevel level, String description)
    {
        PermissionNode<Boolean> node = new PermissionNode<>(Perms.MODID, perm, PermissionTypes.BOOLEAN,
                (player, playerUUID, context) -> level.matches(playerUUID));
        node.setInformation(Component.literal(perm), Component.literal(description));

        this.nodeMap.put(perm, node);
        DEFAULTS.put(perm, level);
        nodes = ImmutableSet.copyOf(nodeMap.values());
    }

    private void wrap_children(final CommandNode<CommandSourceStack> node, final String lastNode,
            final DefaultPermissionLevel prevLevel)
    {
        this.checkedPerm = false;
        if (node.getRequirement() != null) node.getRequirement().test(PermNodes.testPlayer.createCommandSourceStack());
        final String perm = lastNode + "." + node.getName();
        if (!this.checkedPerm)
        {
            this.registerNode(perm, prevLevel, "auto generated perm for argument " + node.getName());
            if (Perms.config.debug) Perms.LOGGER.debug("New perm needed for " + perm);
            final Predicate<CommandSourceStack> req = (cs) -> {
                return CommandManager.hasPerm(cs, "thutperms." + perm);
            };
            try
            {
                PermissionsManager.REQFIELD.set(node, req);
            }
            catch (IllegalArgumentException | IllegalAccessException e)
            {}
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
            boolean permed = node.getRequirement().test(PermNodes.testPlayer.createCommandSourceStack());
            if (!all) all = permed;
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
                if (Perms.config.debug) Perms.LOGGER.debug("New perm needed for " + perm);
                final Predicate<CommandSourceStack> req = (cs) -> {
                    return CommandManager.hasPerm(cs, "thutperms." + perm);
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

        for (final CommandNode<CommandSourceStack> child : node.getChildren()) this.wrap(child, node, source);
    }

    private void wrapCommands(final MinecraftServer server,
            final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        // shouldn't be null, but might be if something goes funny connecting to
        // servers.
        if (server == null) return;
        PermNodes.testPlayer = FakePlayerFactory.get(server.getLevel(Level.OVERWORLD), PermNodes.testProfile);

        this.renames.clear();
        final CommandNode<CommandSourceStack> root = commandDispatcher.getRoot();
        // First wrap and rename the commands.
        this.wrap(root, null, server.createCommandSourceStack());

        // Process the renamed commands.
        this.renames.forEach((o, n) -> {
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
        for (final CommandNode<CommandSourceStack> node : root.getChildren()) if (node instanceof LiteralCommandNode<?>)
        {
            final LiteralCommandNode<?> literal = (LiteralCommandNode<?>) node;
            final java.util.List<LiteralArgumentBuilder<CommandSourceStack>> builders = Lists.newArrayList();
            for (final String s : Perms.config.getAlternates(literal.getLiteral()))
            {
                if (Perms.config.debug) Perms.LOGGER.info("Generating Alternate {} for {}", s, literal.getLiteral());
                builders.add(Commands.literal(s).requires(node.getRequirement()).redirect(node));
            }
            if (!builders.isEmpty()) newNodes.put(node, builders);
        }
        newNodes.forEach((node, list) -> {
            for (final LiteralArgumentBuilder<CommandSourceStack> builder : list) commandDispatcher.register(builder);
        });
    }

    public static final ResourceLocation ID = new ResourceLocation("thutperms:manager");

    @Override
    public ResourceLocation getIdentifier()
    {
        return ID;
    }

    @Override
    public Set<PermissionNode<?>> getRegisteredNodes()
    {
        return nodes;
    }

    @Override
    public <T> T getPermission(ServerPlayer player, PermissionNode<T> node, PermissionDynamicContext<?>... context)
    {
        checkedPerm = true;
        if (node.getType() == PermissionTypes.BOOLEAN)
        {
            @SuppressWarnings("unchecked")
            final T value = (T) GroupManager.get_instance().hasPermission(player.getUUID(),
                    (PermissionNode<Boolean>) node);
            if (Perms.config.debug)
                Perms.LOGGER.info("permnode: " + node.getNodeName() + " " + player.getGameProfile() + " " + value);
            return value;
        }
        else if (node.getType() == PermissionTypes.INTEGER)
        {
            @SuppressWarnings("unchecked")
            T perm = (T) GroupManager.get_instance().getIntPerm(player.getUUID(), (PermissionNode<Integer>) node);
            if (Perms.config.debug)
                Perms.LOGGER.info("permnode: " + node.getNodeName() + " " + player.getGameProfile() + " " + perm);
            if (perm != null) return perm;
        }
        else if (node.getType() == PermissionTypes.STRING)
        {
            @SuppressWarnings("unchecked")
            T perm = (T) GroupManager.get_instance().getStringPerm(player.getUUID(), (PermissionNode<String>) node);
            if (Perms.config.debug)
                Perms.LOGGER.info("permnode: " + node.getNodeName() + " " + player.getGameProfile() + " " + perm);
            if (perm != null) return perm;
        }
        return node.getDefaultResolver().resolve(player, player.getUUID(), context);
    }

    @Override
    public <T> T getOfflinePermission(UUID player, PermissionNode<T> node, PermissionDynamicContext<?>... context)
    {
        checkedPerm = true;
        if (node.getType() == PermissionTypes.BOOLEAN)
        {
            @SuppressWarnings("unchecked")
            final T value = (T) GroupManager.get_instance().hasPermission(player, (PermissionNode<Boolean>) node);
            if (Perms.config.debug) Perms.LOGGER.info("permnode: " + node.getNodeName() + " " + player + " " + value);
            return value;
        }
        else if (node.getType() == PermissionTypes.INTEGER)
        {
            @SuppressWarnings("unchecked")
            T perm = (T) GroupManager.get_instance().getIntPerm(player, (PermissionNode<Integer>) node);
            if (Perms.config.debug) Perms.LOGGER.info("permnode: " + node.getNodeName() + " " + player + " " + perm);
            if (perm != null) return perm;
        }
        return node.getDefaultResolver().resolve(null, player, context);
    }

    public static <T> DefaultPermissionLevel getDefaultPermissionLevel(PermissionNode<T> permission)
    {
        @SuppressWarnings("unchecked")
        T thing = (T) DEFAULTS.get(permission.getNodeName());
        if (thing == null)
        {
            DEFAULTS.put(permission.getNodeName(),
                    thing = permission.getDefaultResolver().resolve(null, PermNodes.testProfile.getId()));
        }
        if (permission.getType() == PermissionTypes.BOOLEAN)
            return (boolean) thing ? DefaultPermissionLevel.ALL : DefaultPermissionLevel.OP;
        return DefaultPermissionLevel.OP;
    }
}
