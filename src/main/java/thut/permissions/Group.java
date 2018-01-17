package thut.permissions;

import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class Group extends PermissionsHolder
{
    public String      name;
    public String      prefix          = "";
    public String      suffix          = "";
    public Set<UUID>   members         = Sets.newHashSet();

    public Group(String name)
    {
        this.name = name;
        for (ICommand command : FMLCommonHandler.instance().getMinecraftServerInstance().getCommandManager()
                .getCommands().values())
        {
            if (command instanceof CommandBase)
            {
                CommandBase base = (CommandBase) command;
                if (base.getRequiredPermissionLevel() <= 0)
                {
                    allowedCommands.add(command.getClass().getName());
                }
            }
        }
    }
}
