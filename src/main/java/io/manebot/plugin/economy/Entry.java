package io.manebot.plugin.economy;

import io.manebot.database.Database;
import io.manebot.plugin.Plugin;
import io.manebot.plugin.PluginType;
import io.manebot.plugin.economy.command.BalanceCommand;
import io.manebot.plugin.economy.command.PayCommand;
import io.manebot.plugin.economy.database.model.Balance;
import io.manebot.plugin.economy.database.model.Transaction;
import io.manebot.plugin.java.PluginEntry;

import java.util.Arrays;

public final class Entry implements PluginEntry {
    @Override
    public void instantiate(Plugin.Builder builder) {
        builder.setType(PluginType.DEPENDENCY);

        final Database database = builder.addDatabase("economy", modelConstructor -> modelConstructor
                .addDependency(modelConstructor.getSystemDatabase())
                .registerEntity(Balance.class)
                .registerEntity(Transaction.class)
        );

        builder.setInstance(Economy.class, plugin -> new Economy(plugin, database));

        builder.addCommand(
                Arrays.asList("balance", "bal"),
                future -> new BalanceCommand(future.getPlugin().getInstance(Economy.class))
        );

        builder.addCommand(
                "pay",
                future -> new PayCommand(future.getPlugin().getInstance(Economy.class))
        );
    }
}
