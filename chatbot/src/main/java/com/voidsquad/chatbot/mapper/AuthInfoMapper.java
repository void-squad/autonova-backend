package com.voidsquad.chatbot.mapper;

import com.voidsquad.chatbot.dto.AuthInfoDTO;
import com.voidsquad.chatbot.service.auth.AuthInfo;
import org.springframework.stereotype.Component;

/**
 * Maps internal AuthInfo to external AuthInfoDTO for safe return to callers.
 */
@Component
public class AuthInfoMapper {

    public AuthInfoDTO toDto(AuthInfo info) {
        if (info == null) return null;
        return AuthInfoDTO.builder()
                .scheme(info.getScheme())
                .userId(info.getUserId())
                .email(info.getEmail())
                .role(info.getRole())
                .firstName(info.getFirstName())
                .valid(info.isValid())
                .error(info.getError())
                .build();
    }
}
