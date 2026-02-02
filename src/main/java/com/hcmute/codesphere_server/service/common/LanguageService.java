package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.LanguageEntity;
import com.hcmute.codesphere_server.model.payload.response.LanguageResponse;
import com.hcmute.codesphere_server.repository.common.LanguageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LanguageService {

    private final LanguageRepository languageRepository;

    public List<LanguageResponse> getAllLanguages() {
        List<LanguageEntity> languages = languageRepository.findAll();
        return languages.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private LanguageResponse mapToResponse(LanguageEntity entity) {
        return LanguageResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .version(entity.getVersion())
                .build();
    }
}

