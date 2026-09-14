package org.nosql.controller;

import org.nosql.dto.DateFilterRequest;
import org.nosql.model.IndividualExpense;
import org.nosql.repository.IndividualExpenseRepository;
import org.nosql.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/individual")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class IndividualExpenseController {

    @Autowired
    private IndividualExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    // MongoTemplate lets us write dynamic queries when standard Repositories aren't enough
    @Autowired
    private MongoTemplate mongoTemplate;

    // --- 1. ADD EXPENSE ---
    @PostMapping("/add-expense")
    public ResponseEntity<?> addExpense(@RequestBody IndividualExpense expense) {
        try {
            if (expense.getAddedBy() == null || expense.getCategory() == null ||
                    expense.getDescription() == null || expense.getType() == null) {
                return ResponseEntity.status(400).body(Map.of("message", "All fields are required!"));
            }
            if (expense.getAmount() == null || expense.getAmount() <= 0) {
                return ResponseEntity.status(400).body(Map.of("message", "Amount must be a positive number!"));
            }

            IndividualExpense savedExpense = expenseRepository.save(expense);
            return ResponseEntity.ok(savedExpense);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Server Error"));
        }
    }

    // --- 2. GET EXPENSES BY DATE (DYNAMIC QUERY) ---
    @PostMapping("/getExpenseBydate")
    public ResponseEntity<?> getExpenseByDate(@RequestBody DateFilterRequest request) {
        try {
            Query query = new Query();

            // 1. Filter by User ID
            query.addCriteria(Criteria.where("addedBy").is(request.getUserid()));

            // 2. Filter by Type (if not "all")
            if (request.getType() != null && !"all".equalsIgnoreCase(request.getType())) {
                query.addCriteria(Criteria.where("type").is(request.getType()));
            }

            // 3. Filter by Date (moment().subtract() equivalent)
            if ("custom".equals(request.getFrequency())) {
                query.addCriteria(Criteria.where("date")
                        .gte(request.getSelectedDate().get(0))
                        .lte(request.getSelectedDate().get(1)));
            } else {
                // Java equivalent of moment().subtract(frequency, 'd').toDate()
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -Integer.parseInt(request.getFrequency()));
                Date pastDate = cal.getTime();

                query.addCriteria(Criteria.where("date").gt(pastDate));
            }

            // Execute the dynamic query
            List<IndividualExpense> transactions = mongoTemplate.find(query, IndividualExpense.class);
            return ResponseEntity.ok(transactions);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Server Error"));
        }
    }

    // --- 3. GET ALL EXPENSES FOR USER (WITH POPULATE) ---
    @GetMapping("/get-expenses/{id}")
    public ResponseEntity<?> getExpenses(@PathVariable String id) {
        try {
            List<IndividualExpense> expenses = expenseRepository.findByAddedBy(id);

            // Replaces: .populate("addedBy", { name: 1, _id: 1 })
            List<Map<String, Object>> response = new ArrayList<>();
            for (IndividualExpense expense : expenses) {
                Map<String, Object> expenseMap = new HashMap<>();
                expenseMap.put("id", expense.getId());
                expenseMap.put("amount", expense.getAmount());
                expenseMap.put("type", expense.getType());
                expenseMap.put("category", expense.getCategory());
                expenseMap.put("description", expense.getDescription());
                expenseMap.put("date", expense.getDate());

                // Fetch the user manually to populate
                userRepository.findById(expense.getAddedBy()).ifPresent(user -> {
                    expenseMap.put("addedBy", Map.of("id", user.getId(), "name", user.getName()));
                });

                response.add(expenseMap);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Server Error"));
        }
    }

    // --- 4. DELETE EXPENSE ---
    @DeleteMapping("/delete-expense/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable String id) {
        try {
            expenseRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Expense Deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Server Error"));
        }
    }
}