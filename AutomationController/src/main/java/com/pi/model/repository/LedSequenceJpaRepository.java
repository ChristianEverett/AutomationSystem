package com.pi.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pi.model.LedSequence;

@Repository(RepositoryType.LedSequence)
public interface LedSequenceJpaRepository extends JpaRepository<LedSequence, String>
{

}
