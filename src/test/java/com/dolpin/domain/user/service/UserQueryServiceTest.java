package com.dolpin.domain.user.service;

import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.repository.UserRepository;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserQueryService 테스트")
class UserQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserQueryServiceImpl userQueryService;

    @Nested
    @DisplayName("findByProviderAndProviderId 메서드 테스트")
    class FindByProviderAndProviderIdTest {

        @Test
        @DisplayName("Repository 위임이 정상적으로 동작한다")
        void findByProviderAndProviderId_DelegatesToRepository() {
            // given
            String provider = "kakao";
            Long providerId = 12345L;
            User expectedUser = User.builder()
                    .id(1L)
                    .providerId(providerId)
                    .provider(provider)
                    .username("testuser")
                    .build();

            given(userRepository.findByProviderAndProviderId(provider, providerId))
                    .willReturn(Optional.of(expectedUser));

            // when
            Optional<User> result = userQueryService.findByProviderAndProviderId(provider, providerId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(expectedUser);
            verify(userRepository).findByProviderAndProviderId(provider, providerId);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 빈 Optional을 반환한다")
        void findByProviderAndProviderId_WithNonExistentUser_ReturnsEmpty() {
            // given
            String provider = "kakao";
            Long providerId = 99999L;
            given(userRepository.findByProviderAndProviderId(provider, providerId))
                    .willReturn(Optional.empty());

            // when
            Optional<User> result = userQueryService.findByProviderAndProviderId(provider, providerId);

            // then
            assertThat(result).isEmpty();
            verify(userRepository).findByProviderAndProviderId(provider, providerId);
        }
    }

    @Nested
    @DisplayName("getUserById 메서드 테스트")
    class GetUserByIdTest {

        @Test
        @DisplayName("유효한 ID로 사용자를 정상적으로 조회한다")
        void getUserById_WithValidId_ReturnsUser() {
            // given
            Long userId = 1L;
            User expectedUser = User.builder()
                    .id(userId)
                    .providerId(12345L)
                    .provider("kakao")
                    .username("testuser")
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(expectedUser));

            // when
            User result = userQueryService.getUserById(userId);

            // then
            assertThat(result).isEqualTo(expectedUser);
            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 BusinessException을 발생시킨다")
        void getUserById_WithNonExistentId_ThrowsBusinessException() {
            // given
            Long nonExistentId = 999L;
            given(userRepository.findById(nonExistentId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userQueryService.getUserById(nonExistentId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .isEqualTo(ResponseStatus.USER_NOT_FOUND);

            verify(userRepository).findById(nonExistentId);
        }
    }

    @Nested
    @DisplayName("existsByUsername 메서드 테스트")
    class ExistsByUsernameTest {

        @Test
        @DisplayName("존재하는 사용자명에 대해 true를 반환한다")
        void existsByUsername_WithExistingUsername_ReturnsTrue() {
            // given
            String existingUsername = "existinguser";
            given(userRepository.existsByUsername(existingUsername)).willReturn(true);

            // when
            boolean result = userQueryService.existsByUsername(existingUsername);

            // then
            assertThat(result).isTrue();
            verify(userRepository).existsByUsername(existingUsername);
        }

        @Test
        @DisplayName("존재하지 않는 사용자명에 대해 false를 반환한다")
        void existsByUsername_WithNonExistentUsername_ReturnsFalse() {
            // given
            String nonExistentUsername = "nonexistentuser";
            given(userRepository.existsByUsername(nonExistentUsername)).willReturn(false);

            // when
            boolean result = userQueryService.existsByUsername(nonExistentUsername);

            // then
            assertThat(result).isFalse();
            verify(userRepository).existsByUsername(nonExistentUsername);
        }
    }
}
