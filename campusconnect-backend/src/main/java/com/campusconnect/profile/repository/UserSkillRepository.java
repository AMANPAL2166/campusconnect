package com.campusconnect.profile.repository;

import com.campusconnect.profile.domain.SkillKind;
import com.campusconnect.profile.domain.UserSkill;
import com.campusconnect.profile.domain.UserSkillId;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserSkillRepository extends JpaRepository<UserSkill, UserSkillId> {

    @Query("select new com.campusconnect.profile.repository.SkillRow(us.id.kind, s.name) "
            + "from UserSkill us join Skill s on s.id = us.id.skillId "
            + "where us.id.userId = :userId order by s.name")
    List<SkillRow> findRows(@Param("userId") Long userId);

    @Query("select us.id.skillId from UserSkill us where us.id.userId = :userId and us.id.kind = :kind")
    List<Long> findSkillIds(@Param("userId") Long userId, @Param("kind") SkillKind kind);

    @Modifying
    @Query("delete from UserSkill us where us.id.userId = :userId and us.id.kind = :kind "
            + "and us.id.skillId in :skillIds")
    int deleteSkills(@Param("userId") Long userId, @Param("kind") SkillKind kind,
                     @Param("skillIds") Collection<Long> skillIds);

    @Modifying
    @Query(value = "INSERT INTO user_skills (user_id, skill_id, kind) VALUES (:userId, :skillId, :kind) "
            + "ON DUPLICATE KEY UPDATE user_id = user_id", nativeQuery = true)
    void insertIfAbsent(@Param("userId") Long userId, @Param("skillId") Long skillId, @Param("kind") String kind);
}