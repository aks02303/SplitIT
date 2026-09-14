package org.nosql.repository;

import org.nosql.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    // ADD THIS NEW METHOD:
    Optional<User> findByEmailIgnoreCase(String email);
}