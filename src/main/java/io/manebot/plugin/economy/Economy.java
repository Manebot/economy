package io.manebot.plugin.economy;

import io.manebot.conversation.Conversation;
import io.manebot.database.Database;
import io.manebot.plugin.Plugin;
import io.manebot.plugin.PluginReference;
import io.manebot.plugin.economy.database.model.Balance;
import io.manebot.plugin.economy.database.model.Transaction;
import io.manebot.user.User;

import java.sql.SQLException;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class Economy implements PluginReference {
    private final Plugin plugin;
    private final Database database;

    private NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
    private boolean enabled = false;

    public Economy(Plugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public Database getDatabase() {
        return database;
    }

    public List<Balance> getTopBalances(Conversation conversation, int max) {
        if (!enabled) throw  new IllegalStateException("Plugin is not enabled.");

        return database.execute(s -> {
            return s.createQuery(
                    "SELECT x FROM " + Balance.class.getName() + " x " +
                            "inner join x.user u " +
                            "WHERE x.balance >= 0.01 AND u.id in (:id) " +
                            "ORDER BY x.balance DESC",
                    Balance.class
            ).setParameter(
                    "id",
                    conversation.getChat().getUsers().stream()
                            .map(x -> (io.manebot.database.model.User) x)
                            .map(io.manebot.database.model.User::getUserId)
                            .collect(Collectors.toList())
            ).setMaxResults(max).getResultList();
        });
    }

    public Balance getAccount(User user) {
        if (!enabled) throw  new IllegalStateException("Plugin is not enabled.");

        Balance balance = database.execute(s -> {
            return s.createQuery(
                    "SELECT x FROM " + Balance.class.getName() + " x " +
                            "inner join x.user u " +
                            "WHERE u.id=:id",
                    Balance.class
            ).setParameter("id", ((io.manebot.database.model.User)user).getUserId())
                    .getResultList()
                    .stream()
                    .findFirst()
                    .orElse(null);
        });

        if (balance == null)
            try {
                balance = database.executeTransaction(s -> {
                    Balance newBalance = new Balance(database, (io.manebot.database.model.User) user);
                    s.persist(newBalance);
                    return newBalance;
                });
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        return balance;
    }

    @Override
    public void load(Plugin.Future plugin) {
        enabled = true;
        format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag(getPlugin().getProperty("currency", "en-us")));
    }

    @Override
    public void unload(Plugin.Future plugin) {
        enabled = false;
    }

    /**
     * Formats a given amount of currency.
     * @param amount Amount to format.
     * @return Formatted amount.
     */
    public String format(double amount) {
        return format(amount, false);
    }

    /**
     * Formats a given amount of currency.
     * @param amount Amount to format.
     * @param change show as financial net change
     * @return Formatted amount.
     */
    public String format(double amount, boolean change) {
        amount = Math.floor(amount * 100D) / 100D;

        return ((change || amount < 0D) ? ((amount < 0D) ? "-" : "+") : "") + format.format(amount)
                .replace("(", "").replace(")", "");
    }
}
