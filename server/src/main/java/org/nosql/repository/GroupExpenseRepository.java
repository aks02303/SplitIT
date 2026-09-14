package org.nosql.repository;

import org.nosql.model.GroupExpense;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupExpenseRepository extends MongoRepository<GroupExpense, String> {
    // Replaces: GroupExpense.find({ group: groupId })
    List<GroupExpense> findByGroup(String groupId);

    // Replaces: GroupExpense.countDocuments({ group: group._id })
    long countByGroup(String groupId);
}