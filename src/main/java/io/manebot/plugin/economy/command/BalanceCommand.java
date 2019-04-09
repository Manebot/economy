package io.manebot.plugin.economy.command;

import io.manebot.chat.TextStyle;
import io.manebot.command.CommandSender;
import io.manebot.command.exception.CommandArgumentException;
import io.manebot.command.exception.CommandExecutionException;
import io.manebot.command.executor.chained.AnnotatedCommandExecutor;
import io.manebot.command.executor.chained.argument.CommandArgumentLabel;
import io.manebot.command.executor.chained.argument.CommandArgumentPage;
import io.manebot.command.executor.chained.argument.CommandArgumentString;
import io.manebot.command.search.CommandArgumentSearch;
import io.manebot.database.model.User;
import io.manebot.database.search.*;
import io.manebot.database.search.handler.SearchHandlerPropertyContains;
import io.manebot.database.search.handler.SearchHandlerPropertyEquals;
import io.manebot.platform.Platform;
import io.manebot.plugin.economy.Economy;
import io.manebot.plugin.economy.database.model.Balance;
import io.manebot.plugin.economy.database.model.Transaction;
import io.manebot.security.Grant;
import io.manebot.user.UserGroup;
import io.manebot.virtual.Virtual;

import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

public class BalanceCommand extends AnnotatedCommandExecutor {
    private final Economy economy;
    private final SearchHandler<Transaction> searchHandler;

    public BalanceCommand(Economy instance) {
        this.economy = instance;
        this.searchHandler = instance.getDatabase()
                .createSearchHandler(Transaction.class)
                .string(new SearchHandlerPropertyContains("reason"))
                .always(clause ->
                        clause.addPredicate(
                                SearchOperator.MERGE,
                                clause.getCriteriaBuilder().equal(
                                        clause.getRoot().get("user"),
                                        (User) Virtual.getInstance().currentProcess().getUser()
                                )
                        )
                )
                .always(clause ->
                        clause.addPredicate(
                                SearchOperator.MERGE,
                                clause.getCriteriaBuilder().equal(
                                        clause.getRoot().get("voided"),
                                        false
                                )
                        )
                )
                .sort("date", "created")
                .sort("net", "net")
                .sort("balance", "balance")
                .defaultSort("date", SortOrder.DESCENDING)
                .build();
    }

    @Command(description = "Gets the top financial rankings",
            permission = "economy.balance.top",
            defaultGrant = Grant.ALLOW)
    public void balance(CommandSender sender)
            throws CommandExecutionException {
        Balance balance = economy.getAccount(sender.getUser());
        if (balance == null || balance.getBalance() == null)
            throw new CommandArgumentException("You do not have a balance.");

        sender.sendMessage("Balance: " + economy.format(balance.getBalance().doubleValue()));
    }

    @Command(description = "Gets the top financial rankings",
            permission = "economy.balance.top",
            defaultGrant = Grant.ALLOW)
    public void top(CommandSender sender,
                    @CommandArgumentLabel.Argument(label = "top") String top)
            throws CommandExecutionException {
        List<Balance> balanceList = economy.getTopBalances(sender.getConversation(), 10);

        sender.sendList(
                Balance.class,
                builder ->
                        builder.direct(balanceList)
                                .elementsPerPage(10)
                                .responder((textBuilder, o) -> textBuilder
                                        .append(placement(balanceList.indexOf(o)+1))
                                        .append(": ")
                                        .append(o.getUser().getDisplayName())
                                        .append(" - ")
                                        .append(economy.format(o.getBalance().doubleValue()))
                                        .append(o.getUser() == sender.getUser() ? " (you!)" : "")
                                )
        );
    }

    @Command(description = "Searches balance history",
            permission = "economy.balance.history",
            defaultGrant = Grant.ALLOW)
    public void history(CommandSender sender,
                        @CommandArgumentLabel.Argument(label = "history") String history,
                        @CommandArgumentSearch.Argument() Search search)
            throws CommandExecutionException {
        try {
            sender.sendList(
                    Transaction.class,
                    searchHandler.search(search, sender.getChat().getDefaultPageSize()),
                    (textBuilder, o) -> textBuilder
                            .append(o.getCreatedDate().toString()).append(": ")
                            .append(economy.format(o.getNet().doubleValue(), true))
                            .append(" ... ")
                    .append(o.getReason(), EnumSet.of(TextStyle.ITALICS))
                    .append(" (")
                    .append(economy.format(o.getBalance().doubleValue()))
                    .append(")")
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDescription() {
        return "Manages economy balance";
    }

    private static final String placement(int i) {
        String numString = Integer.toString(i);

        if (numString.endsWith("11")) return numString + "th";
        else if (numString.endsWith("12")) return numString + "th";
        else if (numString.endsWith("13")) return numString + "th";
        else if (numString.endsWith("1")) return numString + "st";
        else if (numString.endsWith("2")) return numString + "nd";
        else if (numString.endsWith("3")) return numString + "rd";
        else return numString + "th";
    }
}
