package thut.perms.management.names;

import com.google.common.collect.Lists;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.LiteralContents;

public class Prefix extends MutableComponent
{

    public Prefix(final String name)
    {
        super(new LiteralContents(name), Lists.newArrayList(), Style.EMPTY);
    }

}
