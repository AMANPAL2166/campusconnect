
package com.campusconnect.auth.repository;

import com.campusconnect.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Modifying
    @Query("""
        update User u
        set u.status = com.campusconnect.auth.domain.UserStatus.ACTIVE,
            u.emailVerifiedAt = :now
        where u.id = :id
          and u.status = com.campusconnect.auth.domain.UserStatus.PENDING_VERIFICATION
        """)
    int activate(@Param("id") Long id, @Param("now") Instant now);
}
