package io.manebot.plugin.economy.database.model;

import io.manebot.database.Database;
import io.manebot.database.model.Conversation;
import io.manebot.database.model.TimedRow;
import io.manebot.database.model.User;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.SQLException;

@Entity
@Table(
        indexes = {
                @Index(columnList = "userId", unique = true),
                @Index(columnList = "balance"),
                @Index(columnList = "frozen")
        },
        uniqueConstraints = {@UniqueConstraint(columnNames ={"userId"})}
)
public class Balance extends TimedRow {
    @Transient
    private final Database database;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column()
    private int backAccountId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "userId")
    private User user;

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean frozen = false;

    public Balance(Database database) {
        this.database = database;
    }

    public Balance(Database database, User user) {
        this(database);

        this.user = user;
    }

    public int getId() {
        return backAccountId;
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Transaction setBalance(BigDecimal newBalance, String reason) {
        try {
            return database.executeTransaction(s -> {
                Balance account = s.find(Balance.class, getId());

                Transaction transaction = new Transaction(
                        database,
                        user,
                        newBalance.subtract(account.balance),
                        newBalance,
                        reason
                );

                account.setUpdated(System.currentTimeMillis());
                account.balance = newBalance;

                s.persist(transaction);

                return transaction;
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Transaction add(BigDecimal net, String reason) {
        return setBalance(balance.add(net), reason);
    }

    public Transaction subtract(BigDecimal net, String reason) {
        return setBalance(balance.subtract(net), reason);
    }

    public Transaction multiply(BigDecimal net, String reason) {
        return setBalance(balance.multiply(net), reason);
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        try {
            this.frozen = database.executeTransaction(s -> {
                Balance account = s.find(Balance.class, getId());
                account.setUpdated(System.currentTimeMillis());
                return account.frozen = frozen;
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
