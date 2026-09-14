package org.nosql.controller;

import org.nosql.model.Group;
import org.nosql.model.GroupExpense;
import org.nosql.model.User;
import org.nosql.repository.GroupExpenseRepository;
import org.nosql.repository.GroupRepository;
import org.nosql.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/group")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class GroupController {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupExpenseRepository groupExpenseRepository;

    @Autowired
    private UserRepository userRepository;

    // --- 1. ADD GROUP ---
    @PostMapping("/addgroup")
    public ResponseEntity<?> addGroup(@RequestBody Group group) {
        try {
            if (group.getName() == null || group.getMembers() == null) {
                return ResponseEntity.status(400).body(Map.of("error", "Missing required fields"));
            }
            Group savedGroup = groupRepository.save(group);
            return ResponseEntity.status(201).body(Map.of(
                    "success", true,
                    "message", "Group registered successfully",
                    "data", savedGroup
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Internal Server Error"));
        }
    }

    // --- 2. GET GROUPS BY MEMBER ID ---
    @GetMapping("/member/{memberId}")
    public ResponseEntity<?> getGroupsByMember(@PathVariable String memberId) {
        // Find all groups where the members array contains this memberId
        List<Group> groups = groupRepository.findByMembersContaining(memberId);

        // In Node, you mapped over the groups to add 'totalExpenses'.
        // We do the exact same thing using Java Streams and Maps.
        List<Map<String, Object>> responseList = new ArrayList<>();
        for (Group group : groups) {
            long totalExpenses = groupExpenseRepository.countByGroup(group.getId());

            Map<String, Object> groupData = new HashMap<>();
            groupData.put("id", group.getId());
            groupData.put("name", group.getName());
            groupData.put("description", group.getDescription());
            groupData.put("members", group.getMembers());
            groupData.put("totalExpenses", totalExpenses);

            responseList.add(groupData);
        }
        return ResponseEntity.ok(responseList);
    }

    // --- 3. GET SPECIFIC GROUP DETAILS ---
    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroupById(@PathVariable String groupId) {
        Optional<Group> groupOptional = groupRepository.findById(groupId);
        if (groupOptional.isEmpty()) {
            return ResponseEntity.status(404).body("Group not found");
        }

        Group group = groupOptional.get();
        long totalExpenses = groupExpenseRepository.countByGroup(group.getId());

        // Replaces .populate("members", { password: 0 })
        // We fetch the Users manually, and map them to dictionaries without passwords
        List<Map<String, Object>> populatedMembers = new ArrayList<>();
        for (String memberId : group.getMembers()) {
            userRepository.findById(memberId).ifPresent(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("name", user.getName());
                userMap.put("email", user.getEmail());
                populatedMembers.add(userMap);
            });
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", group.getId());
        response.put("name", group.getName());
        response.put("description", group.getDescription());
        response.put("members", populatedMembers);
        response.put("totalExpenses", totalExpenses);

        return ResponseEntity.ok(response);
    }

    // --- 4. DELETE GROUP ---
    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable String groupId) {
        if (!groupRepository.existsById(groupId)) {
            return ResponseEntity.status(404).body("Group not found");
        }
        groupRepository.deleteById(groupId);
        return ResponseEntity.ok("Group Deleted");
    }

    // (Note: The endpoints to add/remove members from a group and updateMemberBalances
    // rely on calculateSplit logic which we will tackle in the ExpenseService next!)
}