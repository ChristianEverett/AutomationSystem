/**
 * 
 */
package com.pi.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Christian Everett
 *
 */
@Repository
public interface TimerRepository extends CrudRepository<Timer, Long>
{
	//public Timer findByCommand(String command);
}
