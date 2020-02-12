package thut.perms.management.names;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class NameEvent extends PlayerEvent
{
    public List<String> prefixes = Lists.newArrayList();
    public List<String> suffixes = Lists.newArrayList();

    public NameEvent(final PlayerEntity player)
    {
        super(player);
    }

}
