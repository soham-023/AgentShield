package com.agentshield.repository;

import com.agentshield.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity.
 *
 * JpaRepository<User, UUID> gives us these methods for free:
 *   - save(user)           → INSERT or UPDATE
 *   - findById(id)         → SELECT by primary key
 *   - findAll()            → SELECT all rows
 *   - deleteById(id)       → DELETE by primary key
 *   - count()              → COUNT(*)
 *   - existsById(id)       → EXISTS check
 *
 * Spring Data JPA generates the implementation at runtime.
 * We just declare the interface — no SQL needed!
 *
 * Custom query methods follow a naming convention:
 *   findByEmail → SELECT * FROM users WHERE email = ?
 *   existsByEmail → SELECT EXISTS(... WHERE email = ?)
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
