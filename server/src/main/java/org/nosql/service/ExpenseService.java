package org.nosql.service;

import org.nosql.model.GroupExpense;
import org.nosql.model.User;
import org.springframework.stereotype.Service;

import java.util.*;

// @Service tells Spring this class contains business logic and can be @Autowired anywhere
@Service
public class ExpenseService {

    // --- 1. CALCULATE SPLIT ---
    // Replaces the calculateSplit JS function
    public List<GroupExpense.MemberBalance> calculateSplit(String paidBy, List<User> members, Double amount) {
        List<GroupExpense.MemberBalance> balances = new ArrayList<>();
        if (members == null || members.isEmpty()) return balances;

        // In JS: +Number(amount / members.length).toFixed(2)
        double splittedAmount = amount / members.size();

        for (User member : members) {
            GroupExpense.MemberBalance mb = new GroupExpense.MemberBalance();
            mb.setMemberId(member.getId());
            mb.setName(member.getName());

            if (member.getId().equals(paidBy)) {
                double balance = amount - splittedAmount;
                // String.format("%.2f", ...) is the exact equivalent of .toFixed(2) in JS
                mb.setBalance(String.format(Locale.US, "%.2f", balance));
            } else {
                mb.setBalance(String.format(Locale.US, "-%.2f", splittedAmount));
            }
            balances.add(mb);
        }
        return balances;
    }

    // --- 2. UPDATE MEMBER BALANCES ---
    // In Node, you returned an array of { expenseId, membersBalance }.
    // We create a quick inner class to represent that shape.
    public static class UpdatedBalance {
        public String expenseId;
        public List<GroupExpense.MemberBalance> membersBalance;

        public UpdatedBalance(String expenseId, List<GroupExpense.MemberBalance> membersBalance) {
            this.expenseId = expenseId;
            this.membersBalance = membersBalance;
        }
    }

    public List<UpdatedBalance> updateMemberBalances(List<GroupExpense> expenses, List<User> members) {
        List<UpdatedBalance> updatedBalances = new ArrayList<>();
        if (expenses != null) {
            for (GroupExpense expense : expenses) {
                List<GroupExpense.MemberBalance> newBalances = calculateSplit(expense.getPaidBy(), members, expense.getAmount());
                updatedBalances.add(new UpdatedBalance(expense.getId(), newBalances));
            }
        }
        return updatedBalances;
    }

    // --- 3. SIMPLIFY DEBTS ---
    // Input structure expected from the controller
    public static class ExpenseItem {
        public String payer;
        public List<String> participants;
        public Double amount;

        public ExpenseItem(String payer, List<String> participants, Double amount) {
            this.payer = payer;
            this.participants = participants;
            this.amount = amount;
        }
    }

    // Output structure returned to the frontend
    public static class SimplifiedDebt {
        public String from;
        public String to;
        public Double amount;

        public SimplifiedDebt(String from, String to, Double amount) {
            this.from = from;
            this.to = to;
            this.amount = Math.round(amount * 100.0) / 100.0; // Keep to 2 decimal places
        }
    }

    public List<SimplifiedDebt> simplifyDebts(List<ExpenseItem> expenses) {
        Map<String, Double> balances = new HashMap<>();
        List<SimplifiedDebt> simplifiedDebts = new ArrayList<>();

        if (expenses == null || expenses.isEmpty()) return simplifiedDebts;

        // Step 1: Calculate total positive (creditor) and negative (debtor) balances
        for (ExpenseItem expense : expenses) {
            balances.put(expense.payer, balances.getOrDefault(expense.payer, 0.0) + expense.amount);

            Double splitAmount = expense.amount / expense.participants.size();
            for (String participant : expense.participants) {
                balances.put(participant, balances.getOrDefault(participant, 0.0) - splitAmount);
            }
        }

        // Step 2: Simplify balances using the greedy algorithm
        while (true) {
            String maxCreditor = null;
            String maxDebtor = null;
            double maxCredit = 0;
            double maxDebit = 0;

            for (Map.Entry<String, Double> entry : balances.entrySet()) {
                if (entry.getValue() > maxCredit) {
                    maxCredit = entry.getValue();
                    maxCreditor = entry.getKey();
                }
                if (entry.getValue() < maxDebit) {
                    maxDebit = entry.getValue();
                    maxDebtor = entry.getKey();
                }
            }

            // Break condition: if no one owes anything larger than 1 cent
            if (maxCreditor == null || maxDebtor == null || maxCredit < 0.01) {
                break;
            }

            double settleAmount = Math.min(maxCredit, Math.abs(maxDebit));

            // Adjust the balances
            balances.put(maxCreditor, balances.get(maxCreditor) - settleAmount);
            balances.put(maxDebtor, balances.get(maxDebtor) + settleAmount);

            simplifiedDebts.add(new SimplifiedDebt(maxDebtor, maxCreditor, settleAmount));
        }

        return simplifiedDebts;
    }
}