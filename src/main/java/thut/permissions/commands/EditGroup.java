package thut.permissions.commands;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import thut.permissions.Group;
import thut.permissions.GroupManager;
import thut.permissions.PermissionsHolder;
import thut.permissions.ThutPerms;
import thut.permissions.util.BaseCommand;

public class EditGroup extends BaseCommand
{
    Map<String, TextFormatting> charCodeMap = Maps.newHashMap();

    String format(String input)
    {
        boolean done = false;
        int index = 0;
        while (!done && index < input.length() && index >= 0)
        {
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
                    String toReplace = input.substring(index, index + 2);
                    String num = toReplace.replace("&", "");
                    TextFormatting format = charCodeMap.get(num);
                    if (format != null) input = input.replaceAll(toReplace, format + "");
                    else index++;
                }
                else
                {
                    done = true;
                }
            }
            catch (Exception e)
            {
                done = true;
                e.printStackTrace();
            }
        }
        return input;
    }

    public EditGroup()
    {
        super(CommandManager.editGroup);
        Field temp = ReflectionHelper.findField(TextFormatting.class, "formattingCode", "field_96329_z", "z");
        temp.setAccessible(true);
        for (TextFormatting format : TextFormatting.values())
        {
            try
            {
                char code = temp.getChar(format);
                charCodeMap.put(code + "", format);
            }
            catch (IllegalArgumentException | IllegalAccessException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getUsage(ICommandSource sender)
    {
        return super.getUsage(sender) + " <add|reset|clear|!set> <arguments>";
    }

    /** Return whether the specified command parameter index is a username
     * parameter. */
    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        if (args[0].equalsIgnoreCase("add")) return index == 1;
        if (args[0].equalsIgnoreCase("remove")) return index == 1;
        return false;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        if (args.length >= 3 && args[0].equalsIgnoreCase("!set"))
        {
            if (args[1].equalsIgnoreCase("parent"))
            {
                if (args.length != 4)
                    throw new CommandException("useage: " + super.getUsage(sender) + " !set parent <child> <parent>");
                String childName = args[2];
                String parentName = args[3];

                PermissionsHolder child = GroupManager.get_instance()._groupNameMap.get(childName);
                Group parent = GroupManager.get_instance()._groupNameMap.get(parentName);

                if (parent == null) throw new CommandException("No group with name " + parent);
                if (child == null)
                {
                    sender.sendMessage(new StringTextComponent(
                            "No group with name " + childName + ", using player perm instead."));
                    UUID id = null;
                    GameProfile profile = null;
                    try
                    {
                        PlayerEntity test = getPlayer(server, sender, childName);
                        id = test.getUniqueID();
                        profile = test.getGameProfile();
                    }
                    catch (Exception e1)
                    {
                        try
                        {
                            id = UUID.fromString(childName);
                        }
                        catch (Exception e)
                        {
                        }
                        profile = new GameProfile(id, childName);
                        profile = TileEntitySkull.updateGameprofile(profile);
                    }
                    if (profile.getId() == null) { throw new CommandException(
                            "Error, cannot find profile for " + childName); }
                    child = GroupManager.get_instance().getPlayerGroup(profile.getId());
                    if (child == null) child = GroupManager.get_instance()._manager.createPlayer(profile.getId());
                }
                child._parent = parent;
                child.parentName = parentName;
                ThutPerms.savePerms();
                sender.sendMessage(new StringTextComponent("Set parent of " + childName + " to " + parentName));
                return;
            }
            if (args.length >= 3 && (args[1].equals("suffix")))
            {
                String groupName = args[2];
                Group g = ThutPerms.getGroup(groupName);
                String arg = "";
                if (g == null) { throw new CommandException("Error, Specifed group does not exist."); }
                if (args.length > 3)
                {
                    arg = args[3];
                    for (int i = 4; i < args.length; i++)
                    {
                        arg = arg + " " + args[i];
                    }
                }
                g.suffix = format(arg);
                sender.sendMessage(new StringTextComponent("Set suffix to " + g.suffix));
                for (UUID id : g.members)
                {
                    PlayerEntity player = server.getPlayerList().getPlayerByUUID(id);
                    if (player != null)
                    {
                        player.refreshDisplayName();
                    }
                }
                ThutPerms.savePerms();
                return;
            }
            if (args.length >= 3 && (args[1].equals("prefix")))
            {
                String groupName = args[2];
                Group g = ThutPerms.getGroup(groupName);
                String arg = "";
                if (g == null) { throw new CommandException("Error, Specifed group does not exist."); }
                if (args.length > 3)
                {
                    arg = args[3];
                    for (int i = 4; i < args.length; i++)
                    {
                        arg = arg + " " + args[i];
                    }
                }
                g.prefix = format(arg);
                sender.sendMessage(new StringTextComponent("Set prefix to " + g.prefix));
                for (UUID id : g.members)
                {
                    PlayerEntity player = server.getPlayerList().getPlayerByUUID(id);
                    if (player != null)
                    {
                        player.refreshDisplayName();
                    }
                }
                ThutPerms.savePerms();
                return;
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("add"))
        {
            String groupName = args[2];
            String playerName = args[1];
            Group g = ThutPerms.getGroup(groupName);
            if (g == null) { throw new CommandException(
                    "Error, specified Group does not exist, try: <player> <group>"); }
            UUID id = null;
            GameProfile profile = null;
            try
            {
                PlayerEntity test = getPlayer(server, sender, playerName);
                id = test.getUniqueID();
                profile = test.getGameProfile();
            }
            catch (Exception e1)
            {
                try
                {
                    id = UUID.fromString(playerName);
                }
                catch (Exception e)
                {
                }
                profile = new GameProfile(id, playerName);
                profile = TileEntitySkull.updateGameprofile(profile);
            }
            if (profile.getId() == null) { throw new CommandException("Error, cannot find profile for " + playerName); }
            Group old = GroupManager.get_instance().getPlayerGroup(profile.getId());
            if (old != null) old.members.remove(profile.getId());
            GroupManager.get_instance()._groupIDMap.remove(profile.getId());
            ThutPerms.addToGroup(profile.getId(), groupName);
            sender.sendMessage(new StringTextComponent("Added " + playerName + " to " + groupName));
            PlayerEntity player = server.getPlayerList().getPlayerByUUID(profile.getId());
            if (player != null) player.refreshDisplayName();
            ThutPerms.savePerms();
            return;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove"))
        {
            String playerName = args[1];
            UUID id = null;
            GameProfile profile = null;
            try
            {
                PlayerEntity test = getPlayer(server, sender, playerName);
                id = test.getUniqueID();
                profile = test.getGameProfile();
            }
            catch (Exception e1)
            {
                try
                {
                    id = UUID.fromString(playerName);
                }
                catch (Exception e)
                {
                }
                profile = new GameProfile(id, playerName);
                profile = TileEntitySkull.updateGameprofile(profile);
            }
            if (profile.getId() == null) { throw new CommandException("Error, cannot find profile for " + playerName); }
            Group old = GroupManager.get_instance().getPlayerGroup(profile.getId());
            if (old != null) old.members.remove(profile.getId());
            GroupManager.get_instance()._groupIDMap.remove(profile.getId());
            ThutPerms.addToGroup(profile.getId(), GroupManager.get_instance().initial.name);
            PlayerEntity player = server.getPlayerList().getPlayerByUUID(profile.getId());
            if (player != null) player.refreshDisplayName();
            ThutPerms.savePerms();
            return;
        }
        else if (args.length == 2 && args[0].equals("reset"))
        {
            String groupName = args[1];
            Group g = ThutPerms.getGroup(groupName);
            if (g == null) { throw new CommandException("Error, Group not found, please create it first."); }
            g.getAllowedCommands().clear();
            g.getBannedCommands().clear();
            g.setAll(false);
            g._init = false;
            g.setAll_non_op(true);
            sender.sendMessage(new StringTextComponent("Reset Permissions for " + groupName));
            ThutPerms.savePerms();
            return;
        }
        else if (args.length == 2 && args[1].equals("clear"))
        {
            String groupName = args[0];
            Group g = ThutPerms.getGroup(groupName);
            if (g == null) { throw new CommandException("Error, Group not found, please create it first."); }
            g.getAllowedCommands().clear();
            g.getBannedCommands().clear();
            g.setAll(false);
            g._init = false;
            sender.sendMessage(new StringTextComponent("Cleared Permissions for " + groupName));
            ThutPerms.savePerms();
            return;
        }
        throw new CommandException(getUsage(sender));
    }

}
