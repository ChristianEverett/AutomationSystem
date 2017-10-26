package com.pi.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pi.model.ActionProfile;
import com.pi.model.DeviceState;
import com.pi.model.DeviceStateDAO;

@Repository(RepositoryType.DeviceState)
public interface CurrentDeviceStateJpaRepository extends JpaRepository<DeviceStateDAO, String>
{

}
