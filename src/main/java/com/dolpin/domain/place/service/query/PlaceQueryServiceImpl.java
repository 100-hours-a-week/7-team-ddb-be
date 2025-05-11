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

        // DTO 변환
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
                placeWithDistance.getLongitude(),
                placeWithDistance.getLatitude()
        });

        // DTO 생성 - 이미지 URL 사용
        return PlaceSearchResponse.PlaceDto.builder()
                .id(placeWithDistance.getId())
                .name(placeWithDistance.getName())
                .thumbnail(placeWithDistance.getImageUrl()) // null 대신 이미지 URL 사용
                .distance(formattedDistance)
                .momentCount("0")
                .keywords(List.of())
                .location(locationMap)
                .similarityScore(null)
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

        // 요일별, 영업 유형별 맵 생성
        Map<String, Map<Boolean, PlaceHours>> hoursByDayAndType = new HashMap<>();

        // 데이터 정리: 요일별로 일반 영업시간과 브레이크 타임 분류
        for (PlaceHours hour : placeHours) {
            String englishDay = DayOfWeek.getEnglishCodeByKoreanCode(hour.getDayOfWeek());

            if (!hoursByDayAndType.containsKey(englishDay)) {
                hoursByDayAndType.put(englishDay, new HashMap<>());
            }

            // isBreakTime으로 구분: true면 브레이크 타임, false면 일반 영업시간
            hoursByDayAndType.get(englishDay).put(hour.getIsBreakTime(), hour);
        }

        // 모든 요일에 대한 스케줄 생성
        List<PlaceDetailResponse.Schedule> schedules = new ArrayList<>();
        for (String dayCode : dayCodesEn) {
            Map<Boolean, PlaceHours> dayHoursMap = hoursByDayAndType.getOrDefault(dayCode, new HashMap<>());

            // 일반 영업시간 (isBreakTime = false)
            PlaceHours regularHours = dayHoursMap.get(false);

            // 브레이크 타임 (isBreakTime = true)
            PlaceHours breakHours = dayHoursMap.get(true);

            PlaceDetailResponse.Schedule.ScheduleBuilder builder =
                    PlaceDetailResponse.Schedule.builder().day(dayCode);

            // 일반 영업시간 설정
            if (regularHours != null && regularHours.getOpenTime() != null && regularHours.getCloseTime() != null) {
                builder.hours(regularHours.getOpenTime() + "~" + regularHours.getCloseTime());
            } else {
                builder.hours(null); // 휴무일이거나 정보가 없는 경우 hours를 null로 설정
            }

            // 브레이크 타임 설정
            if (breakHours != null && breakHours.getOpenTime() != null && breakHours.getCloseTime() != null) {
                builder.breakTime(breakHours.getOpenTime() + "~" + breakHours.getCloseTime());
            } else {
                builder.breakTime(null);
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
        String[] daysOfWeek = {"일", "월", "화", "수", "목", "금", "토"};
        String koreanDayOfWeek = daysOfWeek[dayOfWeek - 1];

        // 오늘의 영업 시간 찾기 - 일반 영업시간 (isBreakTime = false)
        Optional<PlaceHours> todayRegularHours = hours.stream()
                .filter(h -> h.getDayOfWeek().equals(koreanDayOfWeek) && !h.getIsBreakTime())
                .findFirst();

        // 오늘의 브레이크 타임 찾기 (isBreakTime = true)
        Optional<PlaceHours> todayBreakHours = hours.stream()
                .filter(h -> h.getDayOfWeek().equals(koreanDayOfWeek) && h.getIsBreakTime())
                .findFirst();

        // 영업 정보가 없는 경우
        if (todayRegularHours.isEmpty()) {
            return "영업 정보 없음";
        }

        PlaceHours regular = todayRegularHours.get();

        // 휴무일인 경우
        if (regular.getOpenTime() == null || regular.getCloseTime() == null) {
            return "휴무일";
        }

        // 현재 시간을 분 단위로 변환
        int currentTimeInMinutes = currentHour * 60 + currentMinute;

        // 일반 영업시간 분 단위 변환
        int regularOpenTimeInMinutes = parseTimeToMinutes(regular.getOpenTime());
        int regularCloseTimeInMinutes = parseTimeToMinutes(regular.getCloseTime());

        // 브레이크 타임 분 단위 변환 (존재하는 경우)
        int breakStartTimeInMinutes = -1;
        int breakEndTimeInMinutes = -1;

        if (todayBreakHours.isPresent()) {
            PlaceHours breakTime = todayBreakHours.get();
            if (breakTime.getOpenTime() != null && breakTime.getCloseTime() != null) {
                breakStartTimeInMinutes = parseTimeToMinutes(breakTime.getOpenTime());
                breakEndTimeInMinutes = parseTimeToMinutes(breakTime.getCloseTime());
            }
        }

        // 브레이크 타임 중인지 확인
        if (breakStartTimeInMinutes != -1 && breakEndTimeInMinutes != -1 &&
                currentTimeInMinutes >= breakStartTimeInMinutes && currentTimeInMinutes < breakEndTimeInMinutes) {
            return "브레이크 타임";
        }

        // 일반 영업시간 체크 (일반적인 경우: openTime < closeTime)
        if (regularOpenTimeInMinutes < regularCloseTimeInMinutes) {
            if (currentTimeInMinutes >= regularOpenTimeInMinutes && currentTimeInMinutes < regularCloseTimeInMinutes) {
                return "영업 중";
            } else {
                return "영업 종료";
            }
        }
        // 자정을 넘어가는 경우 (예: 21:00 ~ 02:00)
        else {
            if (currentTimeInMinutes >= regularOpenTimeInMinutes || currentTimeInMinutes < regularCloseTimeInMinutes) {
                return "영업 중";
            } else {
                return "영업 종료";
            }
        }
    }

    // 시간 문자열을 분 단위로 변환하는 헬퍼 메서드
    private int parseTimeToMinutes(String timeString) {
        try {
            String[] parts = timeString.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            return hour * 60 + minute;
        } catch (Exception e) {
            log.error("시간 파싱 오류: {}", e.getMessage());
            return 0;
        }
    }
}