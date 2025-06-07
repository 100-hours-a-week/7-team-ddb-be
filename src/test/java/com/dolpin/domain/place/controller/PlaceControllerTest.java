package com.dolpin.domain.place.controller;

import com.dolpin.domain.place.dto.response.PlaceCategoryResponse;
import com.dolpin.domain.place.dto.response.PlaceDetailResponse;
import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.service.query.PlaceQueryService;
import com.dolpin.global.constants.TestConstants;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ApiResponse;
import com.dolpin.global.response.ResponseStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PlaceController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
@DisplayName("PlaceController 테스트")
class PlaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlaceQueryService placeQueryService;

    @Nested
    @DisplayName("장소 검색 API 테스트")
    class SearchPlacesTest {

        @Test
        @DisplayName("정상적인 검색어 기반 검색이 성공한다")
        void searchPlaces_WithQuery_ReturnsSuccessResponse() throws Exception {
            String query = TestConstants.CAFE_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            PlaceSearchResponse mockResponse = PlaceSearchResponse.builder()
                    .total(1)
                    .places(List.of(
                            PlaceSearchResponse.PlaceDto.builder()
                                    .id(TestConstants.PLACE_ID_1)
                                    .name(TestConstants.TEST_CAFE_NAME)
                                    .thumbnail(TestConstants.DEFAULT_IMAGE_URL)
                                    .distance(TestConstants.DISTANCE_100M)
                                    .momentCount(5L)
                                    .keywords(List.of(TestConstants.DELICIOUS_KEYWORD, TestConstants.COZY_KEYWORD))
                                    .location(Map.of("type", "Point", "coordinates", new double[]{lng, lat}))
                                    .similarityScore(TestConstants.SIMILARITY_SCORE_HIGH)
                                    .build()
                    ))
                    .build();

            given(placeQueryService.searchPlaces(query, lat, lng, null)).willReturn(mockResponse);

            MvcResult result = mockMvc.perform(get("/api/v1/places/search")
                            .param("query", query)
                            .param("lat", lat.toString())
                            .param("lng", lng.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<PlaceSearchResponse> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<PlaceSearchResponse>>() {});

            assertThat(apiResponse.getMessage()).isEqualTo("get_place_success");
            assertThat(apiResponse.getData().getTotal()).isEqualTo(1);
            assertThat(apiResponse.getData().getPlaces()).hasSize(1);
            assertThat(apiResponse.getData().getPlaces().get(0).getName()).isEqualTo(TestConstants.TEST_CAFE_NAME);

            verify(placeQueryService).searchPlaces(query, lat, lng, null);
        }

        @Test
        @DisplayName("정상적인 카테고리 기반 검색이 성공한다")
        void searchPlaces_WithCategory_ReturnsSuccessResponse() throws Exception {
            String category = TestConstants.CAFE_CATEGORY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            PlaceSearchResponse mockResponse = PlaceSearchResponse.builder()
                    .total(1)
                    .places(List.of(
                            PlaceSearchResponse.PlaceDto.builder()
                                    .id(TestConstants.PLACE_ID_1)
                                    .name(TestConstants.STARBUCKS_NAME)
                                    .thumbnail(TestConstants.DEFAULT_IMAGE_URL)
                                    .distance(TestConstants.DISTANCE_200M)
                                    .momentCount(10L)
                                    .keywords(List.of(TestConstants.CHAIN_STORE_KEYWORD, TestConstants.SPACIOUS_KEYWORD))
                                    .location(Map.of("type", "Point", "coordinates", new double[]{lng, lat}))
                                    .similarityScore(null)
                                    .build()
                    ))
                    .build();

            given(placeQueryService.searchPlaces(null, lat, lng, category)).willReturn(mockResponse);

            MvcResult result = mockMvc.perform(get("/api/v1/places/search")
                            .param("category", category)
                            .param("lat", lat.toString())
                            .param("lng", lng.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<PlaceSearchResponse> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<PlaceSearchResponse>>() {});

            assertThat(apiResponse.getData().getPlaces().get(0).getSimilarityScore()).isNull();

            verify(placeQueryService).searchPlaces(null, lat, lng, category);
        }

        @Test
        @DisplayName("잘못된 파라미터 조합 시 400 에러가 발생한다")
        void searchPlaces_WithInvalidParams_Returns400Error() throws Exception {
            String query = TestConstants.CAFE_SEARCH_QUERY;
            String category = TestConstants.CAFE_CATEGORY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            given(placeQueryService.searchPlaces(query, lat, lng, category))
                    .willThrow(new BusinessException(ResponseStatus.INVALID_PARAMETER, "검색어와 카테고리 중 하나만 선택해주세요"));

            MvcResult result = mockMvc.perform(get("/api/v1/places/search")
                            .param("query", query)
                            .param("category", category)
                            .param("lat", lat.toString())
                            .param("lng", lng.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<Object> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<Object>>() {});

            assertThat(apiResponse.getMessage()).isEqualTo("검색어와 카테고리 중 하나만 선택해주세요");
            assertThat(apiResponse.getData()).isNull();
        }

        @Test
        @DisplayName("빈 검색 결과를 정상적으로 반환한다")
        void searchPlaces_WithEmptyResult_ReturnsEmptyResponse() throws Exception {
            String query = TestConstants.NON_EXISTENT_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            PlaceSearchResponse emptyResponse = PlaceSearchResponse.builder()
                    .total(0)
                    .places(Collections.emptyList())
                    .build();

            given(placeQueryService.searchPlaces(query, lat, lng, null)).willReturn(emptyResponse);

            MvcResult result = mockMvc.perform(get("/api/v1/places/search")
                            .param("query", query)
                            .param("lat", lat.toString())
                            .param("lng", lng.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<PlaceSearchResponse> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<PlaceSearchResponse>>() {});

            assertThat(apiResponse.getData().getTotal()).isEqualTo(0);
            assertThat(apiResponse.getData().getPlaces()).isEmpty();
        }
    }

    @Nested
    @DisplayName("장소 상세 조회 API 테스트")
    class GetPlaceDetailTest {

        @Test
        @DisplayName("정상적인 장소 상세 조회가 성공한다")
        void getPlaceDetail_WithValidId_ReturnsSuccessResponse() throws Exception {
            Long placeId = TestConstants.PLACE_ID_1;
            PlaceDetailResponse mockResponse = PlaceDetailResponse.builder()
                    .id(placeId)
                    .name(TestConstants.TEST_CAFE_NAME)
                    .address(TestConstants.DEFAULT_ROAD_ADDRESS)
                    .thumbnail(TestConstants.DEFAULT_IMAGE_URL)
                    .location(Map.of("type", "Point", "coordinates", new double[]{TestConstants.CENTER_LNG, TestConstants.CENTER_LAT}))
                    .keywords(List.of(TestConstants.COZY_KEYWORD, TestConstants.DELICIOUS_KEYWORD))
                    .description(TestConstants.DEFAULT_DESCRIPTION)
                    .phone(TestConstants.DEFAULT_PHONE)
                    .openingHours(PlaceDetailResponse.OpeningHours.builder()
                            .status(TestConstants.BUSINESS_STATUS_OPEN)
                            .schedules(List.of(
                                    PlaceDetailResponse.Schedule.builder()
                                            .day("mon")
                                            .hours(TestConstants.OPEN_TIME + "~" + TestConstants.CLOSE_TIME)
                                            .breakTime(TestConstants.BREAK_START_TIME + "~" + TestConstants.BREAK_END_TIME)
                                            .build()
                            ))
                            .build())
                    .menu(List.of(
                            PlaceDetailResponse.Menu.builder()
                                    .name(TestConstants.AMERICANO_MENU)
                                    .price(TestConstants.AMERICANO_PRICE)
                                    .build()
                    ))
                    .build();

            given(placeQueryService.getPlaceDetail(placeId)).willReturn(mockResponse);

            MvcResult result = mockMvc.perform(get("/api/v1/places/{place_id}", placeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<PlaceDetailResponse> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<PlaceDetailResponse>>() {});

            assertThat(apiResponse.getMessage()).isEqualTo("get_place_detail_success");
            assertThat(apiResponse.getData().getId()).isEqualTo(placeId);
            assertThat(apiResponse.getData().getName()).isEqualTo(TestConstants.TEST_CAFE_NAME);
            assertThat(apiResponse.getData().getKeywords()).containsExactlyInAnyOrder(TestConstants.COZY_KEYWORD, TestConstants.DELICIOUS_KEYWORD);
            assertThat(apiResponse.getData().getMenu()).hasSize(1);
            assertThat(apiResponse.getData().getOpeningHours().getStatus()).isEqualTo(TestConstants.BUSINESS_STATUS_OPEN);

            verify(placeQueryService).getPlaceDetail(placeId);
        }

        @Test
        @DisplayName("존재하지 않는 장소 ID 조회 시 404 에러가 발생한다")
        void getPlaceDetail_WithNonExistentId_Returns404Error() throws Exception {
            Long nonExistentId = TestConstants.NON_EXISTENT_PLACE_ID;
            given(placeQueryService.getPlaceDetail(nonExistentId))
                    .willThrow(new BusinessException(ResponseStatus.PLACE_NOT_FOUND, "장소를 찾을 수 없습니다"));

            MvcResult result = mockMvc.perform(get("/api/v1/places/{place_id}", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<Object> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<Object>>() {});

            assertThat(apiResponse.getMessage()).isEqualTo("장소를 찾을 수 없습니다");
            assertThat(apiResponse.getData()).isNull();
        }

        @Test
        @DisplayName("잘못된 형식의 place_id에 대해 400 에러가 발생한다")
        void getPlaceDetail_WithInvalidId_Returns400Error() throws Exception {
            String invalidId = "invalid";

            mockMvc.perform(get("/api/v1/places/{place_id}", invalidId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("카테고리 목록 조회 API 테스트")
    class GetAllCategoriesTest {

        @Test
        @DisplayName("정상적인 카테고리 목록 조회가 성공한다")
        void getAllCategories_ReturnsSuccessResponse() throws Exception {
            PlaceCategoryResponse mockResponse = PlaceCategoryResponse.builder()
                    .categories(List.of(TestConstants.CAFE_CATEGORY, TestConstants.RESTAURANT_CATEGORY,
                            TestConstants.BAR_CATEGORY, TestConstants.BAKERY_CATEGORY))
                    .build();

            given(placeQueryService.getAllCategories()).willReturn(mockResponse);

            MvcResult result = mockMvc.perform(get("/api/v1/places/categories")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<PlaceCategoryResponse> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<PlaceCategoryResponse>>() {});

            assertThat(apiResponse.getMessage()).isEqualTo("get_categories_success");
            assertThat(apiResponse.getData().getCategories()).hasSize(4);
            assertThat(apiResponse.getData().getCategories())
                    .containsExactlyInAnyOrder(TestConstants.CAFE_CATEGORY, TestConstants.RESTAURANT_CATEGORY,
                            TestConstants.BAR_CATEGORY, TestConstants.BAKERY_CATEGORY);

            verify(placeQueryService).getAllCategories();
        }

        @Test
        @DisplayName("빈 카테고리 목록을 정상적으로 반환한다")
        void getAllCategories_WithEmptyResult_ReturnsEmptyResponse() throws Exception {
            PlaceCategoryResponse emptyResponse = PlaceCategoryResponse.builder()
                    .categories(Collections.emptyList())
                    .build();

            given(placeQueryService.getAllCategories()).willReturn(emptyResponse);

            MvcResult result = mockMvc.perform(get("/api/v1/places/categories")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<PlaceCategoryResponse> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<PlaceCategoryResponse>>() {});

            assertThat(apiResponse.getData().getCategories()).isEmpty();
        }
    }

    @Nested
    @DisplayName("공통 응답 구조 검증")
    class CommonResponseTest {

        @Test
        @DisplayName("모든 성공 응답은 올바른 ApiResponse 구조를 가진다")
        void allSuccessResponses_HaveCorrectApiResponseStructure() throws Exception {
            PlaceSearchResponse mockSearchResponse = PlaceSearchResponse.builder()
                    .total(0).places(Collections.emptyList()).build();
            given(placeQueryService.searchPlaces(anyString(), any(), any(), isNull())).willReturn(mockSearchResponse);

            MvcResult result = mockMvc.perform(get("/api/v1/places/search")
                            .param("query", "test")
                            .param("lat", TestConstants.CENTER_LAT.toString())
                            .param("lng", TestConstants.CENTER_LNG.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<PlaceSearchResponse> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<PlaceSearchResponse>>() {});

            assertThat(apiResponse.getMessage()).isNotNull();
            assertThat(apiResponse.getData()).isNotNull();
        }

        @Test
        @DisplayName("서비스 예외 발생 시 적절한 에러 응답을 반환한다")
        void serviceException_ReturnsProperErrorResponse() throws Exception {
            given(placeQueryService.getAllCategories())
                    .willThrow(new BusinessException(ResponseStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류"));

            MvcResult result = mockMvc.perform(get("/api/v1/places/categories")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<Object> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<Object>>() {});

            assertThat(apiResponse.getMessage()).isEqualTo("서버 내부 오류");
            assertThat(apiResponse.getData()).isNull();
        }
    }
}
