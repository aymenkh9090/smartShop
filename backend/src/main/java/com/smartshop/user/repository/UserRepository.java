package com.smartshop.user.repository;

import com.smartshop.user.entity.Role;
import com.smartshop.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<User> findByRoleAndEnabledTrueOrderByFirstNameAscLastNameAsc(Role role);

    /**
     * Verrouille les ADMIN actifs (ordre stable par id) : deux administrateurs qui se retirent mutuellement
     * leurs droits en meme temps ne peuvent pas laisser l'application sans aucun ADMIN actif.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.role = com.smartshop.user.entity.Role.ADMIN and u.enabled = true order by u.id")
    List<User> lockActiveAdmins();
}
