package org.nosql.controller;

import org.nosql.dto.AddExpenseRequest;
import org.nosql.model.Group;
import org.nosql.model.GroupExpense;
import org.nosql.model.User;
import org.nosql.repository.GroupExpenseRepository;
import org.nosql.repository.GroupRepository;
import org.nosql.repository.UserRepository;
import org.nosql.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/expense")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ExpenseController {

    @Autowired
    private GroupExpenseRepository groupExpenseRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseService expenseService;

    // --- 1. ADD EXPENSE ---
    @PostMapping("/addExpense")
    public ResponseEntity<?> addExpense(@RequestBody AddExpenseRequest request) {
        if (request.getGroupId() == null || request.getPaidBy() == null || request.getAmount() == null) {
            return ResponseEntity.status(404).body("Fill all the neccessary details");
        }

        Optional<Group> groupOpt = groupRepository.findById(request.getGroupId());
        if (groupOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Group not found");
        }
        Group group = groupOpt.get();

        // Find all users that belong to this group
        List<User> members = new ArrayList<>();
        group.getMembers().forEach(memberId -> {
            userRepository.findById(memberId).ifPresent(members::add);
        });

        // Use our Service to calculate the math!
        List<GroupExpense.MemberBalance> membersBalance =
                expenseService.calculateSplit(request.getPaidBy(), members, request.getAmount());

        GroupExpense expense = new GroupExpense();
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setDate(request.getDate() != null ? request.getDate() : new Date());
        expense.setGroup(request.getGroupId());
        expense.setPaidBy(request.getPaidBy());
        expense.setMembersBalance(membersBalance);

        groupExpenseRepository.save(expense);
        return ResponseEntity.ok(expense);
    }

    // --- 2. GET EXPENSES SORTED BY STATUS ---
    @GetMapping("/group/{groupId}/member/{memberId}")
    public ResponseEntity<?> getGroupExpensesForMember(@PathVariable String groupId, @PathVariable String memberId) {
        List<GroupExpense> expenses = groupExpenseRepository.findByGroup(groupId);

        // We use Java Streams to filter the lists, exactly like .filter() in JavaScript
        List<GroupExpense> activeExpenses = expenses.stream().filter(e ->
                !e.getApprovedBalance().contains(memberId) &&
                        !e.getIsApproved() &&
                        !e.getSettledMembers().contains(memberId) &&
                        !e.getIsSettled()
        ).collect(Collectors.toList());

        List<GroupExpense> approvedExpenses = expenses.stream().filter(e ->
                e.getApprovedBalance().contains(memberId) ||
                        (e.getIsApproved() && !e.getSettledMembers().contains(memberId) && !e.getIsSettled())
        ).collect(Collectors.toList());

        List<GroupExpense> settledExpenses = expenses.stream().filter(e ->
                e.getSettledMembers().contains(memberId) || e.getIsSettled()
        ).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "activeExpenses", activeExpenses,
                "approvedExpenses", approvedExpenses,
                "settledExpenses", settledExpenses
        ));
    }

    // --- 3. SETTLE EXPENSE ---
    @PostMapping("/{expenseId}/settle/{memberId}")
    public ResponseEntity<?> settleExpense(@PathVariable String expenseId, @PathVariable String memberId) {
        Optional<GroupExpense> expenseOpt = groupExpenseRepository.findById(expenseId);
        if (expenseOpt.isEmpty()) return ResponseEntity.status(404).body("Expense not found");

        GroupExpense expense = expenseOpt.get();

        // Toggle logic: If member is in list, remove them. If not, add them.
        if (expense.getSettledMembers().contains(memberId)) {
            expense.getSettledMembers().remove(memberId);
        } else {
            expense.getSettledMembers().add(memberId);
        }

        // Count how many people need to settle (exclude the payer)
        long totalOweMembers = expense.getMembersBalance().stream()
                .filter(m -> !m.getMemberId().equals(expense.getPaidBy()))
                .count();

        if (expense.getSettledMembers().size() == totalOweMembers) {
            expense.setIsSettled(true);
        } else {
            expense.setIsSettled(false);
        }

        groupExpenseRepository.save(expense);
        return ResponseEntity.ok(expense);
    }

    // --- 4. SIMPLIFY DEBTS ---
    @GetMapping("/simplify/{groupId}")
    public ResponseEntity<?> simplifyDebts(@PathVariable String groupId) {
        List<GroupExpense> expenses = groupExpenseRepository.findByGroup(groupId);
        if (expenses == null || expenses.isEmpty()) {
            return ResponseEntity.status(201).body("expense list is empty");
        }

        // Map database entities to the shape our ExpenseService expects
        List<ExpenseService.ExpenseItem> itemsToSimplify = new ArrayList<>();
        for (GroupExpense expense : expenses) {
            List<String> participants = expense.getMembersBalance().stream()
                    .map(GroupExpense.MemberBalance::getMemberId)
                    .collect(Collectors.toList());

            itemsToSimplify.add(new ExpenseService.ExpenseItem(
                    expense.getPaidBy(), participants, expense.getAmount()
            ));
        }

        List<ExpenseService.SimplifiedDebt> simplified = expenseService.simplifyDebts(itemsToSimplify);
        return ResponseEntity.status(201).body(simplified);
    }
}