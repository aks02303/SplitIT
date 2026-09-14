package org.nosql.repository;

import org.nosql.model.Group;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends MongoRepository<Group, String> {
    // Replaces: Group.find({ members: memberId })
    // Spring automatically generates a query that checks if the array contains the string!
    List<Group> findByMembersContaining(String memberId);
}