package com.flossk.tts.repository;

import com.flossk.tts.entity.UserApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserApiKeyRepository extends JpaRepository<UserApiKey, Long> {
    Optional<UserApiKey> findByKey(UUID key);
}
