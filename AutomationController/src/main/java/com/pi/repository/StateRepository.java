/**
 * 
 */
package com.pi.repository;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Christian Everett
 *
 */
@Repository
public interface StateRepository extends CrudRepository<Action, String>
{

}
