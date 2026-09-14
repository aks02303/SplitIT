package org.nosql.repository;

import org.nosql.model.IndividualExpense;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IndividualExpenseRepository extends MongoRepository<IndividualExpense, String> {
    List<IndividualExpense> findByAddedBy(String addedBy);
}