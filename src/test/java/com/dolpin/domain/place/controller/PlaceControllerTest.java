package com.dolpin.domain.place.controller;

import com.dolpin.domain.place.dto.response.PlaceCategoryResponse;
import com.dolpin.domain.place.dto.response.PlaceDetailResponse;
import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.service.query.PlaceQueryService;
import com.dolpin.global.config.TestConfig;
import com.dolpin.global.constants.TestConstants;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ApiResponse;
import com.dolpin.global.response.ResponseStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@WebMvcTest(controllers = PlaceController.class, excludeAutoConfiguration = {})
@AutoConfigureMockMvc(addFilters = false)
@Import(TestConfig.class)
@ActiveProfiles("test")
@DisplayName("PlaceController 테스트")
class PlaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlaceQueryService placeQueryService;

    @Nested
    @DisplayName("GET /api/v1/places/search - 장소 검색")
    class SearchPlacesTest {

        @Test
        @DisplayName("검색어로 장소 검색이 정상 동작한다")
        @WithMockUser(username = "1")
        void searchPlaces_WithQuery_ReturnsSearchResults() throws Exception {
            // Given
            String query = TestConstants.CAFE_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;
            Long userId = TestConstants.USER_ID_1;

            PlaceSearchResponse expectedResponse = createSearchResponse();
            given(placeQueryService.searchPlacesAsync(query, lat, lng, null, userId))
                    .willReturn(Mono.just(expectedResponse));

            // When & Then
            MvcResult result = mockMvc.perform(get("/api/v1/places/search")
                            .param("query", query)
                            .param("lat", lat.toString())
                            .param("lng", lng.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted())
                    .andReturn();

            // 비동기 처리 완료 후 검증
            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(TestConstants.Api.SUCCESS_MESSAGE_SEARCH))
                    .andExpect(jsonPath("$.data.total").value(2))
                    .andExpect(jsonPath("$.data.places").isArray())
                    .andExpect(jsonPath("$.data.places[0].id").value(TestConstants.PLACE_ID_1))
                    .andExpect(jsonPath("$.data.places[0].name").value(TestConstants.STARBUCKS_NAME));

            verify(placeQueryService).searchPlacesAsync(query, lat, lng, null, userId);
        }

        @Test
        @DisplayName("카테고리로 장소 검색이 정상 동작한다")
        @WithMockUser(username = "1")
        void searchPlaces_WithCategory_ReturnsSearchResults() throws Exception {
            // Given
            String category = TestConstants.CAFE_CATEGORY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;
            Long userId = TestConstants.USER_ID_1;

            PlaceSearchResponse expectedResponse = createSearchResponse();
            given(placeQueryService.searchPlacesAsync(null, lat, lng, category, userId))
                    .willReturn(Mono.just(expectedResponse));

            // When & Then
            MvcResult result = mockMvc.perform(get("/api/v1/places/search")
                            .param("category", category)
                            .param("lat", lat.toString())
                            .param("lng", lng.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted())
                    .andReturn();

            // 비동기 처리 완료 후 검증
            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(TestConstants.Api.SUCCESS_MESSAGE_SEARCH))
                    .andExpect(jsonPath("$.data.total").value(2))
                    .andExpect(jsonPath("$.data.places").isArray());

            verify(placeQueryService).searchPlacesAsync(null, lat, lng, category, userId);
        }

        @Test
        @DisplayName("인증되지 않은 사용자도 장소 검색이 가능하다")
        void searchPlaces_WithoutAuthentication_ReturnsSearchResults() throws Exception {
            // Given
            String query = TestConstants.CAFE_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            PlaceSearchResponse expectedResponse = createSearchResponse();
            given(placeQueryService.searchPlacesAsync(query, lat, lng, null, null))
                    .willReturn(Mono.just(expectedResponse));

            // When & Then
            MvcResult result = mockMvc.perform(get("/api/v1/places/search")
                            .param("query", query)
                            .param("lat", lat.toString())
                            .param("lng", lng.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted())
                    .andReturn();

            // 비동기 처리 완료 후 검증
            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(TestConstants.Api.SUCCESS_MESSAGE_SEARCH));

            verify(placeQueryService).searchPlacesAsync(query, lat, lng, null, null);
        }

        @ParameterizedTest
        @CsvSource({
                "맛있는 카페, ,",
                "맛있는 카페, 37.5665,",
                "맛있는 카페, , 126.9780"
        })
        @DisplayName("파라미터가 올바르게 서비스로 전달되는지 확인")
        @WithMockUser(username = "1")
        void searchPlaces_WithMissingRequiredParams_CallsService(String query, Double lat, Double lng) throws Exception {
            // Given
            Long userId = TestConstants.USER_ID_1;

            // 서비스에서 예외를 던지도록 Mock 설정
            given(placeQueryService.searchPlacesAsync(eq(query), eq(lat), eq(lng), isNull(), eq(userId)))
                    .willReturn(Mono.error(new BusinessException(ResponseStatus.INVALID_PARAMETER, "위치 정보가 필요합니다")));

            // When
            mockMvc.perform(get("/api/v1/places/search")
                            .param("query", query)
                            .param("lat", lat != null ? lat.toString() : "")
                            .param("lng", lng != null ? lng.toString() : "")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(request().asyncStarted());

            // Then - 서비스가 올바른 파라미터로 호출되었는지만 확인
            verify(placeQueryService).searchPlacesAsync(eq(query), eq(lat), eq(lng), isNull(), eq(userId));
        }

        @Test
        @DisplayName("빈 검색 결과를 정상적으로 반환한다")
        @WithMockUser(username = "1")
        void searchPlaces_WithNoResults_ReturnsEmptyResults() throws Exception {
            // Given
            String query = TestConstants.NON_EXISTENT_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;
            Long userId = TestConstants.USER_ID_1;

            PlaceSearchResponse emptyResponse = PlaceSearchResponse.builder()
                    .total(0)
                    .places(Collections.emptyList())
                    .build();

            given(placeQueryService.searchPlacesAsync(query, lat, lng, null, userId))
                    .willReturn(Mono.just(emptyResponse));

            // When & Then
            MvcResult result = mockMvc.perform(get("/api/v1/places/search")
                            .param("query", query)
                            .param("lat", lat.toString())
                            .param("lng", lng.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted())
                    .andReturn();

            // 비동기 처리 완료 후 검증
            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(TestConstants.Api.SUCCESS_MESSAGE_SEARCH))
                    .andExpect(jsonPath("$.data.total").value(0))
                    .andExpect(jsonPath("$.data.places").isArray())
                    .andExpect(jsonPath("$.data.places").isEmpty());

            verify(placeQueryService).searchPlacesAsync(query, lat, lng, null, userId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/places/{place_id} - 장소 상세 조회")
    class GetPlaceDetailTest {

        @Test
        @DisplayName("인증된 사용자의 장소 상세 조회가 정상 동작한다")
        @WithMockUser(username = "1")
        void getPlaceDetail_WithAuthentication_ReturnsPlaceDetail() throws Exception {
            // Given
            Long placeId = TestConstants.PLACE_ID_1;
            Long userId = TestConstants.USER_ID_1;

            PlaceDetailResponse expectedResponse = createPlaceDetailResponse(placeId, true);
            given(placeQueryService.getPlaceDetail(placeId, userId))
                    .willReturn(expectedResponse);

            // When & Then
            mockMvc.perform(get("/api/v1/places/{place_id}", placeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(TestConstants.Api.SUCCESS_MESSAGE_DETAIL))
                    .andExpect(jsonPath("$.data.id").value(placeId))
                    .andExpect(jsonPath("$.data.name").value(TestConstants.TEST_CAFE_NAME))
                    .andExpect(jsonPath("$.data.is_bookmarked").value(true))
                    .andExpect(jsonPath("$.data.keywords").isArray())
                    .andExpect(jsonPath("$.data.menu").isArray())
                    .andExpect(jsonPath("$.data.opening_hours").exists())
                    .andExpect(jsonPath("$.data.location").exists());

            verify(placeQueryService).getPlaceDetail(placeId, userId);
        }

        @ParameterizedTest
        @ValueSource(longs = {999L, 1000L, -1L})
        @DisplayName("다양한 장소 ID로 상세 조회가 서비스에 올바르게 전달된다")
        @WithMockUser(username = "1")
        void getPlaceDetail_WithVariousIds_CallsServiceCorrectly(Long placeId) throws Exception {
            // Given
            Long userId = TestConstants.USER_ID_1;
            PlaceDetailResponse expectedResponse = createPlaceDetailResponse(placeId, true);
            given(placeQueryService.getPlaceDetail(placeId, userId))
                    .willReturn(expectedResponse);

            // When & Then
            mockMvc.perform(get("/api/v1/places/{place_id}", placeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(placeId));

            verify(placeQueryService).getPlaceDetail(placeId, userId);
        }

        @Test
        @DisplayName("잘못된 경로 파라미터 형식에 대한 처리")
        @WithMockUser
        void getPlaceDetail_WithInvalidPathVariable_Returns400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/places/invalid-id")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/places/categories - 카테고리 목록 조회")
    class GetAllCategoriesTest {

        @Test
        @DisplayName("카테고리 목록 조회가 정상 동작한다")
        @WithMockUser
        void getAllCategories_ReturnsCategories() throws Exception {
            // Given
            List<String> categories = List.of(
                    TestConstants.CAFE_CATEGORY,
                    TestConstants.RESTAURANT_CATEGORY,
                    TestConstants.BAR_CATEGORY,
                    TestConstants.BAKERY_CATEGORY,
                    TestConstants.FASTFOOD_CATEGORY
            );
            PlaceCategoryResponse expectedResponse = PlaceCategoryResponse.builder()
                    .categories(categories)
                    .build();

            given(placeQueryService.getAllCategories()).willReturn(expectedResponse);

            // When & Then
            mockMvc.perform(get("/api/v1/places/categories")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(TestConstants.Api.SUCCESS_MESSAGE_CATEGORIES))
                    .andExpect(jsonPath("$.data.categories").isArray())
                    .andExpect(jsonPath("$.data.categories.length()").value(5))
                    .andExpect(jsonPath("$.data.categories[0]").value(TestConstants.CAFE_CATEGORY))
                    .andExpect(jsonPath("$.data.categories[1]").value(TestConstants.RESTAURANT_CATEGORY));

            verify(placeQueryService).getAllCategories();
        }

        @Test
        @DisplayName("빈 카테고리 목록도 정상적으로 반환한다")
        @WithMockUser
        void getAllCategories_WithEmptyList_ReturnsEmptyCategories() throws Exception {
            // Given
            PlaceCategoryResponse emptyResponse = PlaceCategoryResponse.builder()
                    .categories(Collections.emptyList())
                    .build();

            given(placeQueryService.getAllCategories()).willReturn(emptyResponse);

            // When & Then
            mockMvc.perform(get("/api/v1/places/categories")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(TestConstants.Api.SUCCESS_MESSAGE_CATEGORIES))
                    .andExpect(jsonPath("$.data.categories").isArray())
                    .andExpect(jsonPath("$.data.categories").isEmpty());

            verify(placeQueryService).getAllCategories();
        }

        @Test
        @DisplayName("인증 여부와 관계없이 카테고리 조회가 가능하다")
        @WithMockUser(username = "1")
        void getAllCategories_WithAuthentication_ReturnsCategories() throws Exception {
            // Given
            List<String> categories = List.of(TestConstants.CAFE_CATEGORY, TestConstants.RESTAURANT_CATEGORY);
            PlaceCategoryResponse expectedResponse = PlaceCategoryResponse.builder()
                    .categories(categories)
                    .build();

            given(placeQueryService.getAllCategories()).willReturn(expectedResponse);

            // When & Then
            mockMvc.perform(get("/api/v1/places/categories")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.categories.length()").value(2));

            verify(placeQueryService).getAllCategories();
        }
    }

    // 테스트 헬퍼 메서드들
    private PlaceSearchResponse createSearchResponse() {
        List<PlaceSearchResponse.PlaceDto> places = List.of(
                PlaceSearchResponse.PlaceDto.builder()
                        .id(TestConstants.PLACE_ID_1)
                        .name(TestConstants.STARBUCKS_NAME)
                        .thumbnail(TestConstants.STARBUCKS_THUMBNAIL)
                        .distance(TestConstants.DISTANCE_100M)
                        .momentCount(TestConstants.MOMENT_COUNT_LOW)
                        .keywords(List.of(TestConstants.CHAIN_STORE_KEYWORD, TestConstants.SPACIOUS_KEYWORD))
                        .location(Map.of("type", "Point", "coordinates", new double[]{TestConstants.CENTER_LNG, TestConstants.CENTER_LAT}))
                        .isBookmarked(true)
                        .similarityScore(TestConstants.SIMILARITY_SCORE_HIGH)
                        .build(),
                PlaceSearchResponse.PlaceDto.builder()
                        .id(TestConstants.PLACE_ID_2)
                        .name(TestConstants.TWOSOME_NAME)
                        .thumbnail(TestConstants.TEST_THUMBNAIL)
                        .distance(TestConstants.DISTANCE_200M)
                        .momentCount(TestConstants.MOMENT_COUNT_LOW)
                        .keywords(List.of(TestConstants.DESSERT_KEYWORD, TestConstants.CAKE_KEYWORD))
                        .location(Map.of("type", "Point", "coordinates", new double[]{TestConstants.SORT_TEST_PLACE2_LNG, TestConstants.SORT_TEST_PLACE2_LAT}))
                        .isBookmarked(false)
                        .similarityScore(TestConstants.SIMILARITY_SCORE_MEDIUM)
                        .build()
        );

        return PlaceSearchResponse.builder()
                .total(2)
                .places(places)
                .build();
    }

    private PlaceDetailResponse createPlaceDetailResponse(Long placeId, Boolean isBookmarked) {
        List<PlaceDetailResponse.Schedule> schedules = List.of(
                PlaceDetailResponse.Schedule.builder()
                        .day("mon")
                        .hours(TestConstants.OPEN_TIME + "~" + TestConstants.CLOSE_TIME)
                        .breakTime(TestConstants.BREAK_START_TIME + "~" + TestConstants.BREAK_END_TIME)
                        .build(),
                PlaceDetailResponse.Schedule.builder()
                        .day("tue")
                        .hours(TestConstants.OPEN_TIME + "~" + TestConstants.CLOSE_TIME)
                        .breakTime(null)
                        .build()
        );

        PlaceDetailResponse.OpeningHours openingHours = PlaceDetailResponse.OpeningHours.builder()
                .status(TestConstants.BUSINESS_STATUS_OPEN)
                .schedules(schedules)
                .build();

        List<PlaceDetailResponse.Menu> menu = List.of(
                PlaceDetailResponse.Menu.builder()
                        .name(TestConstants.AMERICANO_MENU)
                        .price(TestConstants.AMERICANO_PRICE)
                        .build(),
                PlaceDetailResponse.Menu.builder()
                        .name(TestConstants.LATTE_MENU)
                        .price(TestConstants.LATTE_PRICE)
                        .build()
        );

        return PlaceDetailResponse.builder()
                .id(placeId)
                .name(TestConstants.TEST_CAFE_NAME)
                .address(TestConstants.DEFAULT_ROAD_ADDRESS)
                .thumbnail(TestConstants.DEFAULT_IMAGE_URL)
                .location(Map.of("type", "Point", "coordinates", new double[]{TestConstants.CENTER_LNG, TestConstants.CENTER_LAT}))
                .keywords(List.of(TestConstants.COZY_KEYWORD, TestConstants.DELICIOUS_KEYWORD))
                .description(TestConstants.DEFAULT_DESCRIPTION)
                .phone(TestConstants.DEFAULT_PHONE)
                .isBookmarked(isBookmarked)
                .openingHours(openingHours)
                .menu(menu)
                .build();
    }
}
