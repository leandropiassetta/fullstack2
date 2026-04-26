package br.com.jtech.tasklist.config.infra.utils;

import br.com.jtech.tasklist.config.infra.exceptions.UnauthorizedException;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

@UtilityClass
public class AuthenticatedUserHelper {

    public static String getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("No authenticated user found");
        }

        String userId = authentication.getName();

        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("Invalid authenticated user");
        }

        try {
            UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid authenticated user ID format");
        }

        return userId;
    }

}
