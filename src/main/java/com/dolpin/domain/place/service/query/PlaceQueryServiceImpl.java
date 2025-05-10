package com.dolpin.domain.place.service.query;

import com.dolpin.domain.place.client.PlaceAiClient;
import com.dolpin.domain.place.dto.response.*;
import com.dolpin.domain.place.entity.Place;
import com.dolpin.domain.place.entity.PlaceHours;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import com.dolpin.global.util.DayOfWeek;
import com.dolpin.global.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${place.search.default-radius}")
    private double defaultSearchRadius;

    @Override
    @Transactional(readOnly = true)
    public PlaceCategoryResponse getAllCategories() {
        List<String> categories = placeRepository.findDistinctCategories();

        log.info("Retrieved {} categories", categories.size());

        return PlaceCategoryResponse.builder()
                .categories(categories)
                .build();
    }

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

        if (aiResponse == null || aiResponse.getRecommendations() == null || aiResponse.getRecommendations().isEmpty()) {
            log.info("AI service returned no results");
            return Collections.emptyList();
        }

        // 추천된 장소 ID 목록 추출
        List<Long> placeIds = aiResponse.getRecommendations().stream()
                .map(PlaceAiResponse.PlaceRecommendation::getId)
                .collect(Collectors.toList());

        // 유사도 점수 매핑 (장소 ID -> 유사도 점수)
        Map<Long, Double> similarityScores = aiResponse.getRecommendations().stream()
                .collect(Collectors.toMap(
                        PlaceAiResponse.PlaceRecommendation::getId,
                        PlaceAiResponse.PlaceRecommendation::getSimilarityScore
                ));

        // PostGIS를 활용한 위치 기반 필터링 및 거리 계산 (geography 타입 사용)
        List<PlaceWithDistance> nearbyPlaces = placeRepository.findPlacesWithinRadiusByIds(
                placeIds, lat, lng, defaultSearchRadius);

        // 거리 계산 결과 로깅 (디버깅용)
        nearbyPlaces.forEach(place ->
                log.info("Place ID: {}, Name: {}, Distance: {}",
                        place.getId(), place.getName(), place.getDistance())
        );

        // 필터링된 ID가 없으면 빈 결과 반환
        if (nearbyPlaces.isEmpty()) {
            log.info("No places found within radius");
            return Collections.emptyList();
        }

        // 필터링된 장소 ID 목록
        List<Long> filteredPlaceIds = nearbyPlaces.stream()
                .map(PlaceWithDistance::getId)
                .collect(Collectors.toList());

        // 거리 정보 맵 구성
        Map<Long, Double> distanceMap = nearbyPlaces.stream()
                .collect(Collectors.toMap(
                        PlaceWithDistance::getId,
                        PlaceWithDistance::getDistance
                ));

        // 키워드를 포함한 장소 정보 한 번에 조회 (N+1 문제 방지)
        List<Place> placesWithKeywords = placeRepository.findByIdsWithKeywords(filteredPlaceIds);

        // DTO 변환
        List<PlaceSearchResponse.PlaceDto> placeDtos = placesWithKeywords.stream()
                .map(place -> {
                    Double distance = distanceMap.get(place.getId());
                    Double similarityScore = similarityScores.get(place.getId());

                    return convertToPlaceDto(place, distance, similarityScore);
                })
                .collect(Collectors.toList());

        // 유사도 점수 기준 정렬
        placeDtos.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));

        return placeDtos;
    }

    private List<PlaceSearchResponse.PlaceDto> searchByCategory(String category, Double lat, Double lng) {
        // 카테고리로 주변 장소 전체 검색
        List<PlaceWithDistance> searchResults = placeRepository.findPlacesByCategoryWithinRadius(
                category, lat, lng, defaultSearchRadius);

        // DTO 변환 (수정된 부분)
        return searchResults.stream()
                .map(placeWithDistance -> convertToPlaceDtoFromProjection(placeWithDistance, lat, lng))
                .collect(Collectors.toList());
    }

    private PlaceSearchResponse.PlaceDto convertToPlaceDtoFromProjection(PlaceWithDistance placeWithDistance, Double lat, Double lng) {
        // 거리 포맷팅
        String formattedDistance = formatDistance(placeWithDistance.getDistance());

        // 실제 장소의 위치 정보 사용
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("type", "Point");
        locationMap.put("coordinates", new double[]{
                placeWithDistance.getLongitude(),  // 실제 경도(X) 좌표
                placeWithDistance.getLatitude()    // 실제 위도(Y) 좌표
        });

        // DTO 생성
        return PlaceSearchResponse.PlaceDto.builder()
                .id(placeWithDistance.getId())
                .name(placeWithDistance.getName())
                .thumbnail(null) // 이미지 정보 없음
                .distance(formattedDistance)
                .momentCount("0")  // 추후 연동 필요
                .keywords(List.of()) // 키워드 정보 없음
                .location(locationMap)
                .similarityScore(null) // 유사도 점수 없음
                .build();
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
        // 기본 장소 정보 조회
        Place place = placeRepository.findBasicPlaceById(placeId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.PLACE_NOT_FOUND,
                        "장소를 찾을 수 없습니다: " + placeId));

        // 키워드 정보 조회
        Place placeWithKeywords = placeRepository.findByIdWithKeywords(placeId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.PLACE_NOT_FOUND,
                        "장소를 찾을 수 없습니다: " + placeId));
        List<String> keywords = placeWithKeywords.getKeywords().stream()
                .map(pk -> pk.getKeyword().getKeyword())
                .collect(Collectors.toList());

        // 메뉴 정보 조회
        Place placeWithMenus = placeRepository.findByIdWithMenus(placeId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.PLACE_NOT_FOUND,
                        "장소를 찾을 수 없습니다: " + placeId));
        List<PlaceDetailResponse.Menu> menuList = placeWithMenus.getMenus().stream()
                .map(menu -> PlaceDetailResponse.Menu.builder()
                        .name(menu.getMenuName())
                        .price(menu.getPrice())
                        .build())
                .collect(Collectors.toList());

        // 영업시간 정보 조회
        Place placeWithHours = placeRepository.findByIdWithHours(placeId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.PLACE_NOT_FOUND,
                        "장소를 찾을 수 없습니다: " + placeId));
        List<PlaceHours> hours = placeWithHours.getHours();

        // 위치 정보 변환
        Point location = place.getLocation();
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("type", "Point");
        locationMap.put("coordinates", new double[]{location.getX(), location.getY()});

        // 요일별 스케줄 구성
        List<PlaceDetailResponse.Schedule> schedules = buildDaySchedules(hours);

        // 현재 영업 상태 확인
        String status = determineBusinessStatus(hours);

        PlaceDetailResponse.OpeningHours openingHours = PlaceDetailResponse.OpeningHours.builder()
                .status(status)
                .schedules(schedules)
                .build();

        return PlaceDetailResponse.builder()
                .id(place.getId())
                .name(place.getName())
                .address(place.getRoadAddress())
                .thumbnail(place.getImageUrl()) // 썸네일 이미지 추가
                .location(locationMap)
                .keywords(keywords)
                .description(place.getDescription())
                .openingHours(openingHours)
                .phone(place.getPhone())
                .menu(menuList)
                .build();
    }

    private List<PlaceDetailResponse.Schedule> buildDaySchedules(List<PlaceHours> placeHours) {
        // 모든 요일 코드 (영어)
        String[] dayCodesEn = {"mon", "tue", "wed", "thu", "fri", "sat", "sun"};

        // 요일별 맵 생성
        Map<String, PlaceHours> hoursByDay = new HashMap<>();
        for (PlaceHours hour : placeHours) {
            // 한글 요일 코드를 영어 코드로 변환
            String englishDay = DayOfWeek.getEnglishCodeByKoreanCode(hour.getDayOfWeek());
            hoursByDay.put(englishDay, hour);
        }

        // 모든 요일에 대한 스케줄 생성
        List<PlaceDetailResponse.Schedule> schedules = new ArrayList<>();
        for (String dayCode : dayCodesEn) {
            PlaceHours dayHours = hoursByDay.get(dayCode);

            PlaceDetailResponse.Schedule.ScheduleBuilder builder =
                    PlaceDetailResponse.Schedule.builder().day(dayCode);

            if (dayHours != null && dayHours.getOpenTime() != null && dayHours.getCloseTime() != null) {
                builder.hours(dayHours.getOpenTime() + "~" + dayHours.getCloseTime());
            } else {
                // 휴무일이거나 정보가 없는 경우 hours를 null로 설정
                builder.hours(null);
            }

            schedules.add(builder.build());
        }

        return schedules;
    }

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

    private String determineBusinessStatus(List<PlaceHours> hours) {
        if (hours == null || hours.isEmpty()) {
            return "영업 여부 확인 필요";
        }

        // 현재 요일 및 시간 확인
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        // Calendar.DAY_OF_WEEK는 1(일요일)~7(토요일)
        // PlaceHours의 요일을 Calendar 형식으로 변환
        String[] daysOfWeek = {"일", "월", "화", "수", "목", "금", "토"};
        String koreanDayOfWeek = daysOfWeek[dayOfWeek - 1];

        // 오늘의 영업 시간 찾기
        Optional<PlaceHours> todayHours = hours.stream()
                .filter(h -> h.getDayOfWeek().equals(koreanDayOfWeek))
                .findFirst();

        if (todayHours.isEmpty()) {
            return "영업 정보 없음";
        }

        PlaceHours today = todayHours.get();

        // 휴무일인 경우
        if (today.getOpenTime() == null || today.getCloseTime() == null) {
            return "휴무일";
        }

        // 영업 시간 파싱 (예: "09:00" -> 시간과 분)
        int openHour = 0, openMinute = 0, closeHour = 0, closeMinute = 0;

        try {
            String[] openParts = today.getOpenTime().split(":");
            String[] closeParts = today.getCloseTime().split(":");

            openHour = Integer.parseInt(openParts[0]);
            openMinute = Integer.parseInt(openParts[1]);
            closeHour = Integer.parseInt(closeParts[0]);
            closeMinute = Integer.parseInt(closeParts[1]);
        } catch (Exception e) {
            log.error("영업 시간 파싱 오류: {}", e.getMessage());
            return "영업 여부 확인 필요";
        }

        // 현재 시간을 분 단위로 변환
        int currentTimeInMinutes = currentHour * 60 + currentMinute;
        int openTimeInMinutes = openHour * 60 + openMinute;
        int closeTimeInMinutes = closeHour * 60 + closeMinute;

        // 영업 시간 체크 (일반적인 경우: openTime < closeTime)
        if (openTimeInMinutes < closeTimeInMinutes) {
            if (currentTimeInMinutes >= openTimeInMinutes && currentTimeInMinutes < closeTimeInMinutes) {
                return "영업 중";
            } else {
                return "영업 종료";
            }
        }
        // 자정을 넘어가는 경우 (예: 21:00 ~ 02:00)
        else {
            if (currentTimeInMinutes >= openTimeInMinutes || currentTimeInMinutes < closeTimeInMinutes) {
                return "영업 중";
            } else {
                return "영업 종료";
            }
        }
    }
}