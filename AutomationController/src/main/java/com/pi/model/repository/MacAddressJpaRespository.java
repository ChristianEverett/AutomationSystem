package com.pi.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pi.model.MacAddress;

@Repository(RepositoryType.MACAddress)
public interface MacAddressJpaRespository extends JpaRepository<MacAddress, String>
{

}
