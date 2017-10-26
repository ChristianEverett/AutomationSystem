package com.pi.model.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pi.model.DeviceStateRecord;

@Repository
public interface DeviceStateRecordJpaRepository extends JpaRepository<DeviceStateRecord, Long>
{
	public List<DeviceStateRecord> findByDateBetween(LocalDateTime start, LocalDateTime end);
}
