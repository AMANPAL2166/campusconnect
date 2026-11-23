package com.campusconnect.profile.repository;

import com.campusconnect.profile.domain.Skill;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    /**
     * Race-safe "create if missing". If the name exists (even inserted a millisecond ago by someone else),
     * the duplicate-key clause turns the insert into a harmless no-op. No exception, no poisoned transaction.
     * We do NOT use INSERT IGNORE: it would also hide unrelated errors (Phase E trap).
     */
    @Modifying
    @Query(value = "INSERT INTO skills (name) VALUES (:name) ON DUPLICATE KEY UPDATE name = name", nativeQuery = true)
    void insertIfAbsent(@Param("name") String name);

    List<Skill> findByNameIn(Collection<String> names);

    // Index range scan on uk_skills_name. Spring Data escapes % and _ in the prefix as well.
    List<Skill> findByNameStartingWithOrderByNameAsc(String prefix, Pageable pageable);
}