package com.flossk.tts.repository;

import com.flossk.tts.entity.GenerationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GenerationLogRepository extends JpaRepository<GenerationLog, Long> {
}
