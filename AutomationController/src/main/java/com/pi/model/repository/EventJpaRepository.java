package com.pi.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pi.model.EventHandler;

@Repository(RepositoryType.Event)
public interface EventJpaRepository extends JpaRepository<EventHandler, Integer>
{

}
