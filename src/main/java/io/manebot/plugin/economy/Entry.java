package io.manebot.plugin.economy;

import io.manebot.plugin.Plugin;
import io.manebot.plugin.PluginType;
import io.manebot.plugin.java.PluginEntry;

public final class Entry implements PluginEntry {
    @Override
    public Plugin instantiate(Plugin.Builder builder) {
        builder.type(PluginType.DEPENDENCY);



        return builder.build();
    }
}
