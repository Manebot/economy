package io.manebot.plugin.economy.command;

import io.manebot.chat.Chat;
import io.manebot.chat.ChatSender;
import io.manebot.chat.Community;
import io.manebot.command.CommandSender;
import io.manebot.command.exception.CommandAccessException;
import io.manebot.command.exception.CommandArgumentException;
import io.manebot.command.exception.CommandExecutionException;
import io.manebot.command.executor.chained.AnnotatedCommandExecutor;
import io.manebot.command.executor.chained.argument.CommandArgumentNumeric;
import io.manebot.command.executor.chained.argument.CommandArgumentString;
import io.manebot.conversation.Conversation;
import io.manebot.event.EventExecutionException;
import io.manebot.platform.Platform;
import io.manebot.platform.PlatformUser;
import io.manebot.plugin.economy.Economy;
import io.manebot.plugin.economy.database.model.Balance;
import io.manebot.plugin.economy.event.UserPaidEvent;
import io.manebot.plugin.economy.event.UserPayingEvent;
import io.manebot.security.Grant;
import io.manebot.user.User;

import java.math.BigDecimal;
import java.util.logging.Level;

public class PayCommand extends AnnotatedCommandExecutor {
    private final Economy economy;

    public PayCommand(Economy instance) {
        this.economy = instance;
    }

    @Command(description = "Pays another user",
            permission = "economy.pay",
            defaultGrant = Grant.ALLOW)
    public void pay(CommandSender sender,
                    @CommandArgumentString.Argument(label = "username") String username,
                    @CommandArgumentNumeric.Argument() double amount)
            throws CommandExecutionException {
        User otherUser = economy.getPlugin().getBot().getUserManager().getUserByDisplayName(username);
        if (otherUser == null) throw new CommandArgumentException("User not found.");

        if (otherUser.equals(sender.getUser()))
            throw new CommandArgumentException("You cannot pay yourself.");
        else if (otherUser.getBan() != null)
            throw new CommandArgumentException("You cannot pay a banned user.");

        Balance balance = economy.getAccount(sender.getUser());
        if (balance.isFrozen())
            throw new CommandArgumentException("Your account is frozen.");

        Balance otherBalance = economy.getAccount(otherUser);
        if (otherBalance.isFrozen())
            throw new CommandArgumentException(otherUser.getDisplayName() + "'s account is frozen.");

        Community community = sender.getCommunity();
        if (community != null && community.isMember(otherUser)) {
            throw new CommandAccessException(otherUser.getDisplayName() + " is not a member of this community.");
        }

        double totalBalance = balance.getBalance().doubleValue();

        if (amount < 0.01)
            throw new CommandArgumentException("Invalid amount provided.");
        else if (amount > totalBalance)
            throw new CommandArgumentException("You do not have that amount in your account.");

        try {
            if (economy.getPlugin().getBot().getEventDispatcher().execute(new UserPayingEvent(
                    this,
                    economy,
                    sender,
                    balance,
                    otherBalance,
                    BigDecimal.valueOf(amount)
            )).isCanceled())
                return;
        } catch (EventExecutionException e) {
            throw new CommandExecutionException(e);
        }

        balance.subtract(BigDecimal.valueOf(amount), "Paid " + otherUser.getDisplayName());
        otherBalance.add(BigDecimal.valueOf(amount), "Payment from " + sender.getUser().getDisplayName());

        sender.sendMessage("You paid " + economy.format(amount) + " to " + otherUser.getDisplayName() + ".");

        try {
            economy.getPlugin().getBot().getEventDispatcher().execute(new UserPaidEvent(
                    this,
                    economy,
                    sender,
                    balance,
                    otherBalance,
                    BigDecimal.valueOf(amount)
            ));
        } catch (EventExecutionException e) {
            throw new CommandExecutionException(e);
        }

        otherUser.getAssociations(sender.getChat().getPlatform()).forEach(association -> {
            PlatformUser platformUser;
            try {
                platformUser = association.getPlatformUser();
            } catch (IllegalArgumentException ex) {
                // User not found
                economy.getPlugin().getLogger().log(Level.WARNING, "Problem sending private message to user", ex);
                return;
            }
            if (platformUser == null) return;
            Chat privateChat = platformUser.getPrivateChat();
            if (privateChat == null || !privateChat.isConnected()) return;
            Conversation conversation =
                    economy.getPlugin().getBot().getConversationProvider().getConversationByChat(privateChat);
            CommandSender pmSender = otherUser.createSender(conversation, association.getPlatformUser());
            pmSender.sendMessage(sender.getUser().getDisplayName() + " paid you " + economy.format(amount) + ".");
            pmSender.flush();
        });
    }

    @Override
    public String getDescription() {
        return "Pays another user";
    }
}
