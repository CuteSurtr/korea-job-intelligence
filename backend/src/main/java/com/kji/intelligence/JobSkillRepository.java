package com.kji.intelligence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobSkillRepository extends JpaRepository<JobSkill, Long> {

    List<JobSkill> findByJobId(Long jobId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from JobSkill s where s.jobId = :jobId")
    void deleteByJobId(@Param("jobId") Long jobId);
}
