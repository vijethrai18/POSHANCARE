
package com.poshancare.repository;

import com.poshancare.entity.AccountStatus;
import com.poshancare.entity.AppUser;
import com.poshancare.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository
        extends JpaRepository<AppUser, String> {

    Optional<AppUser> findByMobile(String mobile);

    boolean existsByMobile(String mobile);

    List<AppUser> findByRoleAndStatus(UserRole role, AccountStatus status);

    List<AppUser> findByRole(UserRole role);
}
