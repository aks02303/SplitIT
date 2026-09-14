package org.nosql.repository;

import org.nosql.model.PdfDetails;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PdfDetailsRepository extends MongoRepository<PdfDetails, String> {
    List<PdfDetails> findByGroup(String groupId);
}