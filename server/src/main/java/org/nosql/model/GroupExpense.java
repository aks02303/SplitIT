package org.nosql.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "groupexpenses") // Matches your Node collection
public class GroupExpense {

    @Id
    private String id;

    private String description;
    private Double amount;
    private String category;
    private Date date;

    // In Mongoose, these were type: String, ref: "Group" and "User"
    // In Java, we store the MongoDB ObjectId as a standard String.
    private String group;
    private String paidBy;

    // This replaces your 'membersBalance' array of objects
    private List<MemberBalance> membersBalance = new ArrayList<>();

    private List<String> approvedBalance = new ArrayList<>();
    private Boolean isApproved = false;

    private List<String> settledMembers = new ArrayList<>();
    private Boolean isSettled = false;

    // We create a nested class to perfectly match the JSON shape of your membersBalance objects
    @Data
    public static class MemberBalance {
        private String memberId;
        private String name;
        // You used String for balance in Node (e.g., "-50.00"), so we keep it as String here
        private String balance;
    }
}