package thut.perms.management.names;

import com.google.common.collect.Lists;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.LiteralContents;

public class Suffix extends MutableComponent
{

    public Suffix(final String name)
    {
        super(new LiteralContents(name), Lists.newArrayList(), Style.EMPTY);
    }

}
