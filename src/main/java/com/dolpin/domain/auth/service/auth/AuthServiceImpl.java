package com.dolpin.domain.auth.service.auth;

import com.dolpin.domain.auth.dto.response.OAuthUrlResponse;
import com.dolpin.domain.auth.dto.response.RefreshTokenResponse;
import com.dolpin.domain.auth.dto.response.TokenResponse;
import com.dolpin.domain.auth.entity.Token;
import com.dolpin.domain.auth.entity.enums.OAuthProvider;
import com.dolpin.domain.auth.service.oauth.OAuthApiClient;
import com.dolpin.domain.auth.service.oauth.OAuthInfoResponse;
import com.dolpin.domain.auth.service.oauth.OAuthLoginParams;
import com.dolpin.domain.auth.service.oauth.OAuthLoginUrlProvider;
import com.dolpin.domain.auth.service.oauth.OAuthLoginUrlProviderFactory;
import com.dolpin.domain.auth.service.oauth.kakao.KakaoLoginParams;
import com.dolpin.domain.auth.service.token.JwtTokenProvider;
import com.dolpin.domain.auth.service.token.TokenService;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.service.UserCommandService;
import com.dolpin.domain.user.service.UserQueryService;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor // Lombok이 모든 final 필드를 포함하는 생성자를 생성합니다
public class AuthServiceImpl implements AuthService {

    private final OAuthLoginUrlProviderFactory oAuthLoginUrlProviderFactory;
    private final List<OAuthApiClient> oAuthApiClients; // Map 대신 List로 선언
    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;
    private final TokenService tokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public OAuthUrlResponse getOAuthLoginUrl(String provider, String redirectUri) {
        OAuthProvider oAuthProvider = OAuthProvider.fromString(provider);
        OAuthLoginUrlProvider urlProvider = oAuthLoginUrlProviderFactory.getProvider(oAuthProvider);
        return new OAuthUrlResponse(urlProvider.getLoginUrl(redirectUri));
    }

    @Override
    @Transactional
    public TokenResponse generateTokenByAuthorizationCode(String code) {
        // 임시적으로 Kakao로 설정 (향후 제공자 감지 로직 추가 필요)
        OAuthProvider provider = OAuthProvider.KAKAO;

        // 주입받은 List에서 필요한 ApiClient 찾기
        OAuthApiClient apiClient = oAuthApiClients.stream()
                .filter(client -> client.oauthProvider() == provider)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResponseStatus.OAUTH_PROVIDER_NOT_EXIST));

        // 1. 인증 코드로 OAuth 액세스 토큰 요청
        OAuthLoginParams loginParams = createLoginParams(code, provider);
        String oauthAccessToken = apiClient.requestAccessToken(loginParams);

        if (oauthAccessToken == null || oauthAccessToken.isEmpty()) {
            throw new BusinessException(ResponseStatus.UNAUTHORIZED.withMessage("OAuth 인증에 실패했습니다."));
        }

        // 2. OAuth 액세스 토큰으로 사용자 정보 요청
        OAuthInfoResponse userInfo = apiClient.requestUserInfo(oauthAccessToken);

        // 3. 사용자 정보로 회원 찾기 또는 새로 생성
        boolean isNewUser = false;
        User user;

        Optional<User> existingUser = userQueryService.findByProviderAndProviderId(
                userInfo.getProvider(),
                Long.parseLong(userInfo.getProviderId())
        );

        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            user = userCommandService.createUser(userInfo);
            isNewUser = true;  // 신규 사용자로 표시
        }

        // 4. JWT 토큰 생성 (액세스 토큰)
        String accessToken = jwtTokenProvider.generateToken(user.getId());

        // 5. 리프레시 토큰 생성 및 저장
        Token refreshToken = tokenService.createRefreshToken(user);

        // 6. 토큰 응답 생성 및 반환
        return TokenResponse.of(
                accessToken,
                refreshToken.getToken(),
                jwtTokenProvider.getExpirationMs() / 1000,
                user,
                isNewUser
        );
    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshToken(String refreshToken) {
        return tokenService.refreshAccessToken(refreshToken);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        tokenService.invalidateRefreshToken(refreshToken);
    }

    // 사용자 찾기 또는 생성 (내부 메서드)
    private User findOrCreateUser(OAuthInfoResponse userInfo) {
        Optional<User> userOptional = userQueryService.findByProviderAndProviderId(
                userInfo.getProvider(),
                Long.parseLong(userInfo.getProviderId())
        );

        return userOptional.orElseGet(() -> userCommandService.createUser(userInfo));
    }

    // OAuth 로그인 파라미터 생성 (내부 메서드)
    private OAuthLoginParams createLoginParams(String code, OAuthProvider provider) {
        // Kakao 전용 구현 (향후 확장 필요)
        if (provider == OAuthProvider.KAKAO) {
            return new KakaoLoginParams(code, null);
        }

        throw new BusinessException(ResponseStatus.OAUTH_PROVIDER_NOT_EXIST);
    }
}