package com.kuet.minimarket.repository;

import com.kuet.minimarket.entity.Role;
import com.kuet.minimarket.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
