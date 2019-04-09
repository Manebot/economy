package io.manebot.plugin.economy.event;

import io.manebot.command.CommandSender;
import io.manebot.event.CheckedEvent;

import io.manebot.plugin.economy.Economy;
import io.manebot.plugin.economy.database.model.Balance;

import java.math.BigDecimal;

public class UserPayingEvent extends CheckedEvent {
    private final Economy economy;
    private final CommandSender commandSender;
    private final Balance payee, payer;
    private final BigDecimal amount;

    public UserPayingEvent(Object sender,
                           Economy economy,
                           CommandSender commandSender,
                           Balance payer,
                           Balance payee,
                           BigDecimal amount) {
        super(sender);

        this.economy = economy;
        this.commandSender = commandSender;
        this.payee = payee;
        this.payer = payer;
        this.amount = amount;
    }

    public Economy getEconomy() {
        return economy;
    }

    public CommandSender getCommandSender() {
        return commandSender;
    }

    public Balance getPayee() {
        return payee;
    }

    public Balance getPayer() {
        return payer;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
