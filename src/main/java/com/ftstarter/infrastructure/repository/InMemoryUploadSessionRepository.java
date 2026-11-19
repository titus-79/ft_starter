package com.ftstarter.infrastructure.repository;

import com.ftstarter.domain.model.UploadSession;
import com.ftstarter.domain.port.UploadSessionRepository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryUploadSessionRepository implements UploadSessionRepository {

    private final ConcurrentHashMap<String, UploadSession> sessions =
            new ConcurrentHashMap<>();

    @Override
    public UploadSession save(UploadSession session) {
        sessions.put(session.uploadId(), session);
        return session;
    }

    @Override
    public Optional<UploadSession> findById(String uploadId) {
        return Optional.ofNullable(sessions.get(uploadId));
    }
}
