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
public class RefreshTokenResponse {

    private String accessToken;
    private String refreshToken;

    public static RefreshTokenResponse from(AuthTokens authTokens) {
        return RefreshTokenResponse.builder()
                .accessToken(authTokens.getAccessToken())
                .refreshToken(authTokens.getRefreshToken())
                .build();
    }

}
