package com.pi.model.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.pi.model.ActionProfile;

@Repository(RepositoryType.ActionProfile)
public interface ActionProfileJpaRepository extends JpaRepository<ActionProfile, String>
{
	@Query(value = "SELECT name from ActionProfile", nativeQuery = true)
	public Set<String> readActionProfileNames();
}
