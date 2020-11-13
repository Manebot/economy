package io.manebot.plugin.economy.event;

import io.manebot.event.Event;

import io.manebot.plugin.economy.database.model.Balance;
import io.manebot.plugin.economy.database.model.Transaction;

public class BalanceTransactionEvent extends Event {
    private final Balance balance;
    private final Transaction transaction;

    public BalanceTransactionEvent(Object sender,
                                   Balance balance,
                                   Transaction transaction) {
        super(sender);

        this.balance = balance;
        this.transaction = transaction;
    }

    public Balance getBalance() {
        return balance;
    }

    public Transaction getTransaction() {
        return transaction;
    }
}
