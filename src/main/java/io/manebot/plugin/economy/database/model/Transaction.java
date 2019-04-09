package io.manebot.plugin.economy.database.model;

import io.manebot.database.Database;
import io.manebot.database.model.TimedRow;
import io.manebot.database.model.User;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.SQLException;

@Entity
@Table(
        indexes = {
                @Index(columnList = "userId"),
                @Index(columnList = "net"),
                @Index(columnList = "balance"),
                @Index(columnList = "voided")
        }
)
public class Transaction extends TimedRow {
    @Transient
    private final Database database;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column()
    private int transactionId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "userId")
    private User user;

    @Column(nullable = false)
    private BigDecimal net = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = true)
    private String reason;

    @Column(nullable = false)
    private boolean voided = false;

    public Transaction(Database database) {
        this.database = database;
    }

    public Transaction(Database database, User user, BigDecimal net, BigDecimal balance, String reason) {
        this(database);

        this.user = user;
        this.net = net;
        this.balance = balance;
        this.reason = reason;
    }

    public int getId() {
        return transactionId;
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getNet() {
        return net;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getReason() {
        return reason;
    }

    public boolean isVoided() {
        return voided;
    }

    public void setVoided(boolean voided) {
        try {
            this.voided = database.executeTransaction(s -> {
                Transaction transaction = s.find(Transaction.class, getId());
                transaction.setUpdated(System.currentTimeMillis());
                return transaction.voided = voided;
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
