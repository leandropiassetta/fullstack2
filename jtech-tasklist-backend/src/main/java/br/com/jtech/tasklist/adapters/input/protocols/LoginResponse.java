package br.com.jtech.tasklist.adapters.input.protocols;

import br.com.jtech.tasklist.application.core.domains.AuthTokens;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String userId;
    private String name;
    private String email;

    public static LoginResponse from(AuthTokens authTokens) {
        return LoginResponse.builder()
                .accessToken(authTokens.getAccessToken())
                .refreshToken(authTokens.getRefreshToken())
                .userId(authTokens.getUserId())
                .name(authTokens.getName())
                .email(authTokens.getEmail())
                .build();
    }

}
