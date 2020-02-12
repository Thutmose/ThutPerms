package thut.perms.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import net.minecraftforge.fml.config.ModConfig;

@Retention(RetentionPolicy.RUNTIME)
public @interface Configure
{
    String category();

    String comment() default "";

    ModConfig.Type type() default ModConfig.Type.COMMON;
}
