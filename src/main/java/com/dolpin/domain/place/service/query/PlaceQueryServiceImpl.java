package com.dolpin.domain.place.service.query;

import com.dolpin.domain.place.client.PlaceAiClient;
import com.dolpin.domain.place.dto.response.PlaceAiResponse;
import com.dolpin.domain.place.dto.response.PlaceDetailResponse;
import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.dto.response.PlaceWithDistance;
import com.dolpin.domain.place.entity.Place;
import com.dolpin.domain.place.entity.PlaceHours;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import com.dolpin.global.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceQueryServiceImpl implements PlaceQueryService {

    private final PlaceRepository placeRepository;
    private final PlaceAiClient placeAiClient;

    // 기본 검색 반경: 700m
    private static final double DEFAULT_SEARCH_RADIUS = 700.0;

    private static final Map<String, String> DAY_OF_WEEK_MAP = Map.of(
            "mon", "월",
            "tue", "화",
            "wed", "수",
            "thu", "목",
            "fri", "금",
            "sat", "토",
            "sun", "일"
    );

    private static final Map<String, String> REVERSE_DAY_OF_WEEK_MAP = Map.of(
            "월", "mon",
            "화", "tue",
            "수", "wed",
            "목", "thu",
            "금", "fri",
            "토", "sat",
            "일", "sun"
    );

    @Override
    @Transactional(readOnly = true)
    public PlaceSearchResponse searchPlaces(String query, Double lat, Double lng, String category) {

        boolean hasQuery = StringUtils.isNotBlank(query);
        boolean hasCategory = StringUtils.isNotBlank(category);


        // query와 category가 동시에 있으면 에러
        if (hasQuery && hasCategory) {
            throw new BusinessException(
                    ResponseStatus.INVALID_PARAMETER,
                    "검색어와 카테고리 중 하나만 선택해주세요");
        }

        // 검색어와 카테고리 둘 다 없으면 에러
        if (!hasQuery && !hasCategory) {
            throw new BusinessException(
                    ResponseStatus.INVALID_PARAMETER,
                    "검색어 또는 카테고리가 필요합니다");
        }

        // 위치 유효성 검사
        if (lat == null || lng == null) {
            throw new BusinessException(
                    ResponseStatus.INVALID_PARAMETER,
                    "위치 정보가 필요합니다");
        }


        List<PlaceSearchResponse.PlaceDto> placeDtos;

        // 검색어가 있는 경우 AI 검색
        if (query != null && !query.trim().isEmpty()) {
            placeDtos = searchByQuery(query, lat, lng);
        }
        // 카테고리만 있는 경우 DB 직접 검색
        else {
            placeDtos = searchByCategory(category, lat, lng);
        }

        return PlaceSearchResponse.builder()
                .total(placeDtos.size())
                .places(placeDtos)
                .build();
    }

    private List<PlaceSearchResponse.PlaceDto> searchByQuery(String query, Double lat, Double lng) {
        // AI 서비스에 검색 쿼리 전송
        PlaceAiResponse aiResponse = placeAiClient.recommendPlaces(query);

        if (aiResponse == null || aiResponse.getData() == null || aiResponse.getData().isEmpty()) {
            return Collections.emptyList();
        }

        // 추천된 장소 ID 목록 추출
        List<Long> placeIds = aiResponse.getData().stream()
                .map(PlaceAiResponse.PlaceRecommendation::getPlaceId)
                .collect(Collectors.toList());

        // 유사도 점수 매핑 (장소 ID -> 유사도 점수)
        Map<Long, Double> similarityScores = aiResponse.getData().stream()
                .collect(Collectors.toMap(
                        PlaceAiResponse.PlaceRecommendation::getPlaceId,
                        PlaceAiResponse.PlaceRecommendation::getSimilarityScore
                ));

        // PostGIS를 사용하여 반경 내 장소 조회 및 거리 계산
        List<PlaceWithDistance> nearbyPlaces = placeRepository.findPlacesWithinRadiusByIds(
                placeIds, lat, lng, DEFAULT_SEARCH_RADIUS);

        // DTO 변환
        List<PlaceSearchResponse.PlaceDto> placeDtos = nearbyPlaces.stream()
                .map(placeWithDistance -> {
                    Place place = placeWithDistance.getPlace();
                    Double distance = placeWithDistance.getDistance();
                    Double similarityScore = similarityScores.getOrDefault(place.getId(), 0.0);

                    return convertToPlaceDto(place, distance, similarityScore);
                })
                .collect(Collectors.toList());

        // 유사도 점수 기준 정렬
        placeDtos.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));

        return placeDtos;
    }

    private List<PlaceSearchResponse.PlaceDto> searchByCategory(String category, Double lat, Double lng) {
        // 카테고리로 주변 장소 전체 검색 (페이징 없이)
        List<PlaceWithDistance> searchResults = placeRepository.findPlacesByCategoryWithinRadius(
                category, lat, lng, DEFAULT_SEARCH_RADIUS);

        // DTO 변환
        return searchResults.stream()
                .map(placeWithDistance -> convertToPlaceDto(
                        placeWithDistance.getPlace(),
                        placeWithDistance.getDistance(),
                        null))
                .collect(Collectors.toList());
    }

    private PlaceSearchResponse.PlaceDto convertToPlaceDto(Place place, Double distance, Double similarityScore) {
        // 거리 포맷팅
        String formattedDistance = formatDistance(distance);

        // 키워드 추출
        List<String> keywordList = place.getKeywords().stream()
                .map(pk -> pk.getKeyword().getKeyword())
                .collect(Collectors.toList());

        // 위치 정보 변환
        Point location = place.getLocation();
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("type", "Point");
        locationMap.put("coordinates", new double[]{location.getX(), location.getY()});

        // DTO 생성
        return PlaceSearchResponse.PlaceDto.builder()
                .id(place.getId())
                .name(place.getName())
                .thumbnail(place.getImageUrl())
                .distance(formattedDistance)
                .momentCount("0")  // 추후 연동 필요
                .keywords(keywordList)
                .location(locationMap)
                .similarityScore(similarityScore)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PlaceDetailResponse getPlaceDetail(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.PLACE_NOT_FOUND, ("장소를 찾을 수 없습니다."+ placeId)));

        // 위치 정보 변환
        Point location = place.getLocation();
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("type", "Point");
        locationMap.put("coordinates", new double[]{location.getX(), location.getY()});

        // 태그(키워드) 정보
        List<String> keywords = place.getKeywords().stream()
                .map(pk -> pk.getKeyword().getKeyword())
                .collect(Collectors.toList());

        // 메뉴 정보
        List<PlaceDetailResponse.Menu> menuList = place.getMenus().stream()
                .map(menu -> PlaceDetailResponse.Menu.builder()
                        .name(menu.getMenuName())
                        .price(menu.getPrice())
                        .build())
                .collect(Collectors.toList());

        // 영업시간 정보
        List<PlaceDetailResponse.Schedule> schedules = place.getHours().stream()
                .map(this::convertToSchedule)
                .collect(Collectors.toList());

        // 영업상태 설정
        String status = schedules.isEmpty() ? "영업 여부 확인 필요" : "영업 중";

        PlaceDetailResponse.OpeningHours openingHours = PlaceDetailResponse.OpeningHours.builder()
                .status(status)
                .schedules(schedules)
                .build();

        return PlaceDetailResponse.builder()
                .id(place.getId())
                .name(place.getName())
                .address(place.getRoadAddress())
                .location(locationMap)
                .keywords(keywords)
                .description(place.getDescription())
                .openingHours(openingHours)
                .phone(place.getPhone())
                .menu(menuList)
                .build();
    }

    /**
     * 거리 포맷팅 (m 또는 km 단위)
     */
    private String formatDistance(Double distanceInMeters) {
        if (distanceInMeters == null) return "0";

        if (distanceInMeters < 1000) {
            // 미터 단위로 표시
            return Math.round(distanceInMeters) + "m";
        } else {
            // 킬로미터 단위로 표시
            BigDecimal km = BigDecimal.valueOf(distanceInMeters / 1000.0)
                    .setScale(1, RoundingMode.HALF_UP);
            return km.toString() + "km";
        }
    }

    private PlaceDetailResponse.Schedule convertToSchedule(PlaceHours hours) {
        String day = REVERSE_DAY_OF_WEEK_MAP.getOrDefault(hours.getDayOfWeek(), hours.getDayOfWeek().toLowerCase());

        PlaceDetailResponse.Schedule.ScheduleBuilder builder = PlaceDetailResponse.Schedule.builder()
                .day(day);

        if (hours.getOpenTime() != null && hours.getCloseTime() != null) {
            String hoursString = hours.getOpenTime() + "~" + hours.getCloseTime();
            builder.hours(hoursString);
        } else {
            String koreanDay = DAY_OF_WEEK_MAP.getOrDefault(day, hours.getDayOfWeek());
            builder.note("정기휴무 (매주 " + koreanDay + "요일)");
        }

        return builder.build();
    }
}