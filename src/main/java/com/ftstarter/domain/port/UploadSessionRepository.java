package com.ftstarter.domain.port;

import com.ftstarter.domain.model.UploadSession;
import java.util.Optional;

public interface UploadSessionRepository {

    UploadSession save(UploadSession session);

    Optional<UploadSession> findById(String uploadId);
}
