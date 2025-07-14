package com.dolpin.domain.moment.service.ai;

import com.dolpin.domain.moment.client.MomentAiClient;
import com.dolpin.domain.moment.dto.request.AiMomentGenerationRequest;
import com.dolpin.domain.moment.dto.request.MomentCreateRequest;
import com.dolpin.domain.moment.dto.response.AiMomentGenerationResponse;
import com.dolpin.domain.moment.dto.response.MomentCreateResponse;
import com.dolpin.domain.moment.service.command.MomentCommandService;
import com.dolpin.domain.place.dto.response.PlaceDetailResponse;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.domain.place.service.query.PlaceQueryService;
import com.dolpin.global.constants.MomentTestConstants;
import com.dolpin.global.constants.SystemUserConstants;
import com.dolpin.global.constants.PlaceTestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiMomentGenerationService 테스트")
class AiMomentGenerationServiceTest {

    @Mock
    private PlaceQueryService placeQueryService;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private MomentCommandService momentCommandService;

    @Mock
    private MomentAiClient momentAiClient;

    @InjectMocks
    private AiMomentGenerationServiceImpl aiMomentGenerationService;

    private PlaceDetailResponse testPlaceDetail;
    private AiMomentGenerationResponse testAiResponse;
    private MomentCreateResponse testMomentCreateResponse;

    @BeforeEach
    void setUp() {
        testPlaceDetail = createTestPlaceDetail();
        testAiResponse = createTestAiResponse();
        testMomentCreateResponse = createTestMomentCreateResponse();
    }

    @Nested
    @DisplayName("일일 AI 기록 생성 테스트")
    class GenerateDailyMomentTest {

        @Test
        @DisplayName("정상적인 일일 기록 생성")
        void generateDailyMoment_Success() {
            // given
            setupSuccessfulScenario();

            // when
            aiMomentGenerationService.generateDailyMoment();

            // then
            verifySuccessfulExecution();
        }

        @Test
        @DisplayName("사용 가능한 장소가 없는 경우")
        void generateDailyMoment_NoPlacesAvailable() {
            // given
            given(placeRepository.findRandomPlaceIds(1)).willReturn(List.of());

            // when
            aiMomentGenerationService.generateDailyMoment();

            // then
            then(placeQueryService).should(times(0))
                    .getPlaceDetailWithoutBookmark(any(Long.class));
            then(momentAiClient).should(times(0))
                    .generateMomentFromPlace(any(AiMomentGenerationRequest.class));
            then(momentCommandService).should(times(0))
                    .createMoment(any(Long.class), any(MomentCreateRequest.class));
        }

        @Test
        @DisplayName("AI API 호출 실패 - null 응답")
        void generateDailyMoment_AiApiReturnsNull() {
            // given
            given(placeRepository.findRandomPlaceIds(1))
                    .willReturn(List.of(MomentTestConstants.TEST_PLACE_ID));
            given(placeQueryService.getPlaceDetailWithoutBookmark(MomentTestConstants.TEST_PLACE_ID))
                    .willReturn(testPlaceDetail);
            given(momentAiClient.generateMomentFromPlace(any(AiMomentGenerationRequest.class)))
                    .willReturn(null);

            // when
            aiMomentGenerationService.generateDailyMoment();

            // then
            then(momentCommandService).should(times(0))
                    .createMoment(any(Long.class), any(MomentCreateRequest.class));
        }

        @Test
        @DisplayName("AI API 호출 실패 - 예외 발생")
        void generateDailyMoment_AiApiThrowsException() {
            // given
            given(placeRepository.findRandomPlaceIds(1))
                    .willReturn(List.of(MomentTestConstants.TEST_PLACE_ID));
            given(placeQueryService.getPlaceDetailWithoutBookmark(MomentTestConstants.TEST_PLACE_ID))
                    .willReturn(testPlaceDetail);
            given(momentAiClient.generateMomentFromPlace(any(AiMomentGenerationRequest.class)))
                    .willThrow(new RuntimeException(PlaceTestConstants.ERROR_MESSAGE_AI_UNAVAILABLE));

            // when & then
            assertThatThrownBy(() -> aiMomentGenerationService.generateDailyMoment())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(PlaceTestConstants.ERROR_MESSAGE_AI_UNAVAILABLE);

            then(momentCommandService).should(times(0))
                    .createMoment(any(Long.class), any(MomentCreateRequest.class));
        }

        @Test
        @DisplayName("장소 상세 정보 조회 실패")
        void generateDailyMoment_PlaceDetailFetchFailed() {
            // given
            given(placeRepository.findRandomPlaceIds(1))
                    .willReturn(List.of(MomentTestConstants.TEST_PLACE_ID));
            given(placeQueryService.getPlaceDetailWithoutBookmark(MomentTestConstants.TEST_PLACE_ID))
                    .willThrow(new RuntimeException(PlaceTestConstants.ERROR_MESSAGE_PLACE_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> aiMomentGenerationService.generateDailyMoment())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(PlaceTestConstants.ERROR_MESSAGE_PLACE_NOT_FOUND);

            then(momentAiClient).should(times(0))
                    .generateMomentFromPlace(any(AiMomentGenerationRequest.class));
        }

        @Test
        @DisplayName("Moment 생성 실패")
        void generateDailyMoment_MomentCreationFailed() {
            // given
            given(placeRepository.findRandomPlaceIds(1))
                    .willReturn(List.of(MomentTestConstants.TEST_PLACE_ID));
            given(placeQueryService.getPlaceDetailWithoutBookmark(MomentTestConstants.TEST_PLACE_ID))
                    .willReturn(testPlaceDetail);
            given(momentAiClient.generateMomentFromPlace(any(AiMomentGenerationRequest.class)))
                    .willReturn(testAiResponse);
            given(momentCommandService.createMoment(eq(SystemUserConstants.DOLPIN_USER_ID), any(MomentCreateRequest.class)))
                    .willThrow(new RuntimeException(PlaceTestConstants.ERROR_MESSAGE_DATABASE_ERROR));

            // when & then
            assertThatThrownBy(() -> aiMomentGenerationService.generateDailyMoment())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(PlaceTestConstants.ERROR_MESSAGE_DATABASE_ERROR);
        }
    }

    @Nested
    @DisplayName("AI 요청 데이터 변환 테스트")
    class AiRequestConversionTest {

        @Test
        @DisplayName("PlaceDetail에서 AiRequest로 변환 - 전체 정보")
        void buildAiRequest_CompleteData() {
            // given
            setupSuccessfulScenario();

            // when
            aiMomentGenerationService.generateDailyMoment();

            // then
            ArgumentCaptor<AiMomentGenerationRequest> requestCaptor =
                    ArgumentCaptor.forClass(AiMomentGenerationRequest.class);
            then(momentAiClient).should(times(1))
                    .generateMomentFromPlace(requestCaptor.capture());

            AiMomentGenerationRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getId()).isEqualTo(MomentTestConstants.TEST_PLACE_ID);
            assertThat(capturedRequest.getName()).isEqualTo(MomentTestConstants.TEST_PLACE_NAME);
            assertThat(capturedRequest.getAddress()).isEqualTo(PlaceTestConstants.TEST_ADDRESS);
            assertThat(capturedRequest.getThumbnail()).isEqualTo(PlaceTestConstants.TEST_THUMBNAIL);
            assertThat(capturedRequest.getKeyword()).containsExactly(PlaceTestConstants.COZY_KEYWORD, PlaceTestConstants.CAFE_CATEGORY);
            assertThat(capturedRequest.getDescription()).isEqualTo(PlaceTestConstants.TEST_DESCRIPTION);
            assertThat(capturedRequest.getPhone()).isEqualTo(PlaceTestConstants.DEFAULT_PHONE);
        }

        @Test
        @DisplayName("PlaceDetail에서 AiRequest로 변환 - 일부 정보 null")
        void buildAiRequest_PartialData() {
            // given
            PlaceDetailResponse partialPlaceDetail = createPartialPlaceDetail();
            given(placeRepository.findRandomPlaceIds(1))
                    .willReturn(List.of(MomentTestConstants.TEST_PLACE_ID));
            given(placeQueryService.getPlaceDetailWithoutBookmark(MomentTestConstants.TEST_PLACE_ID))
                    .willReturn(partialPlaceDetail);
            given(momentAiClient.generateMomentFromPlace(any(AiMomentGenerationRequest.class)))
                    .willReturn(testAiResponse);
            given(momentCommandService.createMoment(any(Long.class), any(MomentCreateRequest.class)))
                    .willReturn(testMomentCreateResponse);

            // when
            aiMomentGenerationService.generateDailyMoment();

            // then
            ArgumentCaptor<AiMomentGenerationRequest> requestCaptor =
                    ArgumentCaptor.forClass(AiMomentGenerationRequest.class);
            then(momentAiClient).should(times(1))
                    .generateMomentFromPlace(requestCaptor.capture());

            AiMomentGenerationRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getId()).isEqualTo(MomentTestConstants.TEST_PLACE_ID);
            assertThat(capturedRequest.getName()).isEqualTo(MomentTestConstants.TEST_PLACE_NAME);
            assertThat(capturedRequest.getOpeningHours()).isNull();
            assertThat(capturedRequest.getMenu()).isNull();
        }

        @Test
        @DisplayName("영업시간 정보 변환")
        void convertOpeningHours() {
            // given
            setupSuccessfulScenario();

            // when
            aiMomentGenerationService.generateDailyMoment();

            // then
            ArgumentCaptor<AiMomentGenerationRequest> requestCaptor =
                    ArgumentCaptor.forClass(AiMomentGenerationRequest.class);
            then(momentAiClient).should(times(1))
                    .generateMomentFromPlace(requestCaptor.capture());

            AiMomentGenerationRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getOpeningHours()).isNotNull();
            assertThat(capturedRequest.getOpeningHours().getStatus()).isEqualTo(PlaceTestConstants.BUSINESS_STATUS_OPEN);
            assertThat(capturedRequest.getOpeningHours().getSchedules()).hasSize(1);
            assertThat(capturedRequest.getOpeningHours().getSchedules().get(0).getDay()).isEqualTo(PlaceTestConstants.MONDAY);
            assertThat(capturedRequest.getOpeningHours().getSchedules().get(0).getHours()).isEqualTo(PlaceTestConstants.OPEN_TIME + "~" + PlaceTestConstants.TEST_CLOSE_TIME_18);
        }

        @Test
        @DisplayName("메뉴 정보 변환")
        void convertMenu() {
            // given
            setupSuccessfulScenario();

            // when
            aiMomentGenerationService.generateDailyMoment();

            // then
            ArgumentCaptor<AiMomentGenerationRequest> requestCaptor =
                    ArgumentCaptor.forClass(AiMomentGenerationRequest.class);
            then(momentAiClient).should(times(1))
                    .generateMomentFromPlace(requestCaptor.capture());

            AiMomentGenerationRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getMenu()).hasSize(PlaceTestConstants.EXPECTED_MENU_COUNT);
            assertThat(capturedRequest.getMenu().get(0).getName()).isEqualTo(PlaceTestConstants.AMERICANO_MENU);
            assertThat(capturedRequest.getMenu().get(0).getPrice()).isEqualTo(PlaceTestConstants.AMERICANO_PRICE);
            assertThat(capturedRequest.getMenu().get(1).getName()).isEqualTo(PlaceTestConstants.LATTE_MENU);
            assertThat(capturedRequest.getMenu().get(1).getPrice()).isEqualTo(PlaceTestConstants.LATTE_PRICE);
        }
    }

    @Nested
    @DisplayName("MomentCreateRequest 생성 테스트")
    class MomentCreateRequestGenerationTest {

        @Test
        @DisplayName("AI 응답에서 MomentCreateRequest 생성")
        void generateMomentCreateRequest() {
            // given
            setupSuccessfulScenario();

            // when
            aiMomentGenerationService.generateDailyMoment();

            // then
            ArgumentCaptor<MomentCreateRequest> requestCaptor =
                    ArgumentCaptor.forClass(MomentCreateRequest.class);
            then(momentCommandService).should(times(1))
                    .createMoment(eq(SystemUserConstants.DOLPIN_USER_ID), requestCaptor.capture());

            MomentCreateRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getTitle()).isEqualTo(MomentTestConstants.NEW_MOMENT_TITLE);
            assertThat(capturedRequest.getContent()).isEqualTo(MomentTestConstants.NEW_MOMENT_CONTENT);
            assertThat(capturedRequest.getPlaceId()).isEqualTo(MomentTestConstants.TEST_PLACE_ID);
            assertThat(capturedRequest.getPlaceName()).isEqualTo(MomentTestConstants.TEST_PLACE_NAME);
            assertThat(capturedRequest.getImages()).containsExactly(
                    PlaceTestConstants.AI_IMAGE_1, PlaceTestConstants.AI_IMAGE_2);
            assertThat(capturedRequest.getIsPublic()).isTrue();
        }

        @Test
        @DisplayName("돌핀 사용자로 기록 생성 확인")
        void createMomentWithDolpinUser() {
            // given
            setupSuccessfulScenario();

            // when
            aiMomentGenerationService.generateDailyMoment();

            // then
            then(momentCommandService).should(times(1))
                    .createMoment(eq(SystemUserConstants.DOLPIN_USER_ID), any(MomentCreateRequest.class));
        }
    }

    @Nested
    @DisplayName("랜덤 장소 선택 테스트")
    class RandomPlaceSelectionTest {

        @Test
        @DisplayName("정상적인 랜덤 장소 선택")
        void selectRandomPlace_Success() {
            // given
            setupSuccessfulScenario();

            // when
            aiMomentGenerationService.generateDailyMoment();

            // then
            then(placeRepository).should(times(1)).findRandomPlaceIds(1);
        }

        @Test
        @DisplayName("여러 장소 중 첫 번째 선택")
        void selectRandomPlace_MultipleResults() {
            // given
            given(placeRepository.findRandomPlaceIds(1))
                    .willReturn(List.of(MomentTestConstants.TEST_PLACE_ID, 999L, 888L));
            given(placeQueryService.getPlaceDetailWithoutBookmark(MomentTestConstants.TEST_PLACE_ID))
                    .willReturn(testPlaceDetail);
            given(momentAiClient.generateMomentFromPlace(any(AiMomentGenerationRequest.class)))
                    .willReturn(testAiResponse);
            given(momentCommandService.createMoment(any(Long.class), any(MomentCreateRequest.class)))
                    .willReturn(testMomentCreateResponse);

            // when
            aiMomentGenerationService.generateDailyMoment();

            // then
            then(placeQueryService).should(times(1))
                    .getPlaceDetailWithoutBookmark(MomentTestConstants.TEST_PLACE_ID);
        }
    }

    @Nested
    @DisplayName("통합 시나리오 테스트")
    class IntegrationScenarioTest {

        @Test
        @DisplayName("전체 워크플로우 성공 시나리오")
        void completeWorkflow_Success() {
            // given
            setupSuccessfulScenario();

            // when
            aiMomentGenerationService.generateDailyMoment();

            // then
            // 1. 랜덤 장소 선택
            then(placeRepository).should(times(1)).findRandomPlaceIds(1);

            // 2. 장소 상세 정보 조회
            then(placeQueryService).should(times(1))
                    .getPlaceDetailWithoutBookmark(MomentTestConstants.TEST_PLACE_ID);

            // 3. AI API 호출
            then(momentAiClient).should(times(1))
                    .generateMomentFromPlace(any(AiMomentGenerationRequest.class));

            // 4. Moment 생성
            then(momentCommandService).should(times(1))
                    .createMoment(eq(SystemUserConstants.DOLPIN_USER_ID), any(MomentCreateRequest.class));
        }

        @Test
        @DisplayName("중간 단계 실패 시 후속 작업 중단")
        void workflow_StopsOnFailure() {
            // given - AI API 단계에서 실패
            given(placeRepository.findRandomPlaceIds(1))
                    .willReturn(List.of(MomentTestConstants.TEST_PLACE_ID));
            given(placeQueryService.getPlaceDetailWithoutBookmark(MomentTestConstants.TEST_PLACE_ID))
                    .willReturn(testPlaceDetail);
            given(momentAiClient.generateMomentFromPlace(any(AiMomentGenerationRequest.class)))
                    .willReturn(null); // AI 실패

            // when
            aiMomentGenerationService.generateDailyMoment();

            // then
            then(placeRepository).should(times(1)).findRandomPlaceIds(1);
            then(placeQueryService).should(times(1))
                    .getPlaceDetailWithoutBookmark(MomentTestConstants.TEST_PLACE_ID);
            then(momentAiClient).should(times(1))
                    .generateMomentFromPlace(any(AiMomentGenerationRequest.class));

            // Moment 생성은 호출되지 않아야 함
            then(momentCommandService).should(times(0))
                    .createMoment(any(Long.class), any(MomentCreateRequest.class));
        }
    }

    // Helper methods
    private void setupSuccessfulScenario() {
        given(placeRepository.findRandomPlaceIds(1))
                .willReturn(List.of(MomentTestConstants.TEST_PLACE_ID));
        given(placeQueryService.getPlaceDetailWithoutBookmark(MomentTestConstants.TEST_PLACE_ID))
                .willReturn(testPlaceDetail);
        given(momentAiClient.generateMomentFromPlace(any(AiMomentGenerationRequest.class)))
                .willReturn(testAiResponse);
        given(momentCommandService.createMoment(eq(SystemUserConstants.DOLPIN_USER_ID), any(MomentCreateRequest.class)))
                .willReturn(testMomentCreateResponse);
    }

    private void verifySuccessfulExecution() {
        then(placeRepository).should(times(1)).findRandomPlaceIds(1);
        then(placeQueryService).should(times(1))
                .getPlaceDetailWithoutBookmark(MomentTestConstants.TEST_PLACE_ID);
        then(momentAiClient).should(times(1))
                .generateMomentFromPlace(any(AiMomentGenerationRequest.class));
        then(momentCommandService).should(times(1))
                .createMoment(eq(SystemUserConstants.DOLPIN_USER_ID), any(MomentCreateRequest.class));
    }

    private PlaceDetailResponse createTestPlaceDetail() {
        Map<String, Object> location = Map.of(
                "type", "Point",
                "coordinates", new double[]{PlaceTestConstants.CENTER_LNG, PlaceTestConstants.CENTER_LAT}
        );

        PlaceDetailResponse.Schedule schedule = PlaceDetailResponse.Schedule.builder()
                .day(PlaceTestConstants.MONDAY)
                .hours(PlaceTestConstants.OPEN_TIME + "~" + PlaceTestConstants.TEST_CLOSE_TIME_18)
                .breakTime(PlaceTestConstants.LUNCH_BREAK_START + "~" + PlaceTestConstants.LUNCH_BREAK_END)
                .build();

        PlaceDetailResponse.OpeningHours openingHours = PlaceDetailResponse.OpeningHours.builder()
                .status(PlaceTestConstants.BUSINESS_STATUS_OPEN)
                .schedules(List.of(schedule))
                .build();

        PlaceDetailResponse.Menu menu1 = PlaceDetailResponse.Menu.builder()
                .name(PlaceTestConstants.AMERICANO_MENU)
                .price(PlaceTestConstants.AMERICANO_PRICE)
                .build();

        PlaceDetailResponse.Menu menu2 = PlaceDetailResponse.Menu.builder()
                .name(PlaceTestConstants.LATTE_MENU)
                .price(PlaceTestConstants.LATTE_PRICE)
                .build();

        return PlaceDetailResponse.builder()
                .id(MomentTestConstants.TEST_PLACE_ID)
                .name(MomentTestConstants.TEST_PLACE_NAME)
                .address(PlaceTestConstants.TEST_ADDRESS)
                .thumbnail(PlaceTestConstants.TEST_THUMBNAIL)
                .location(location)
                .keywords(List.of(PlaceTestConstants.COZY_KEYWORD, PlaceTestConstants.CAFE_CATEGORY))
                .description(PlaceTestConstants.TEST_DESCRIPTION)
                .phone(PlaceTestConstants.DEFAULT_PHONE)
                .openingHours(openingHours)
                .menu(List.of(menu1, menu2))
                .isBookmarked(false)
                .build();
    }

    private PlaceDetailResponse createPartialPlaceDetail() {
        return PlaceDetailResponse.builder()
                .id(MomentTestConstants.TEST_PLACE_ID)
                .name(MomentTestConstants.TEST_PLACE_NAME)
                .address(PlaceTestConstants.TEST_ADDRESS)
                .thumbnail(PlaceTestConstants.TEST_THUMBNAIL)
                .keywords(List.of(PlaceTestConstants.COZY_KEYWORD))
                .openingHours(null)
                .menu(null)
                .build();
    }

    private AiMomentGenerationResponse createTestAiResponse() {
        return new AiMomentGenerationResponse(
                MomentTestConstants.NEW_MOMENT_TITLE,
                MomentTestConstants.NEW_MOMENT_CONTENT,
                MomentTestConstants.TEST_PLACE_ID,
                List.of(PlaceTestConstants.AI_IMAGE_1, PlaceTestConstants.AI_IMAGE_2),
                true
        );
    }

    private MomentCreateResponse createTestMomentCreateResponse() {
        return MomentCreateResponse.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
