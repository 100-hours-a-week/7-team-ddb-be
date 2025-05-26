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
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
        return PlaceCategoryResponse.builder()
                .categories(categories)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PlaceSearchResponse searchPlaces(String query, Double lat, Double lng, String category) {
        validateSearchParameters(query, category, lat, lng);

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

    private void validateSearchParameters(String query, String category, Double lat, Double lng) {
        boolean hasQuery = StringUtils.isNotBlank(query);
        boolean hasCategory = StringUtils.isNotBlank(category);

        if (hasQuery && hasCategory) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER, "검색어와 카테고리 중 하나만 선택해주세요");
        }

        if (!hasQuery && !hasCategory) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER, "검색어 또는 카테고리가 필요합니다");
        }

        if (lat == null || lng == null) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER, "위치 정보가 필요합니다");
        }
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

        // 유사도 점수 및 키워드 매핑
        Map<Long, Double> similarityScores = aiResponse.getRecommendations().stream()
                .collect(Collectors.toMap(
                        PlaceAiResponse.PlaceRecommendation::getId,
                        PlaceAiResponse.PlaceRecommendation::getSimilarityScore
                ));

        Map<Long, List<String>> keywordsByPlaceId = aiResponse.getRecommendations().stream()
                .filter(rec -> rec.getKeyword() != null && !rec.getKeyword().isEmpty())
                .collect(Collectors.toMap(
                        PlaceAiResponse.PlaceRecommendation::getId,
                        PlaceAiResponse.PlaceRecommendation::getKeyword
                ));

        // PostGIS를 활용한 위치 기반 필터링
        List<PlaceWithDistance> nearbyPlaces = placeRepository.findPlacesWithinRadiusByIds(
                placeIds, lat, lng, defaultSearchRadius);

        if (nearbyPlaces.isEmpty()) {
            log.info("No places found within radius");
            return Collections.emptyList();
        }

        // 한 번에 모든 키워드 정보 조회 (N+1 해결)
        List<Long> filteredPlaceIds = nearbyPlaces.stream()
                .map(PlaceWithDistance::getId)
                .collect(Collectors.toList());

        Map<Long, Double> distanceMap = nearbyPlaces.stream()
                .collect(Collectors.toMap(PlaceWithDistance::getId, PlaceWithDistance::getDistance));

        // 한 번의 쿼리로 모든 키워드 정보 조회
        List<Place> placesWithKeywords = placeRepository.findByIdsWithKeywords(filteredPlaceIds);

        // DTO 변환
        List<PlaceSearchResponse.PlaceDto> placeDtos = placesWithKeywords.stream()
                .map(place -> {
                    Double distance = distanceMap.get(place.getId());
                    Double similarityScore = similarityScores.get(place.getId());

                    // AI 키워드 우선, 없으면 DB 키워드 사용
                    List<String> keywords = keywordsByPlaceId.getOrDefault(place.getId(),
                            place.getKeywords().stream()
                                    .map(pk -> pk.getKeyword().getKeyword())
                                    .collect(Collectors.toList())
                    );

                    return convertToPlaceDto(place, distance, similarityScore, keywords);
                })
                .collect(Collectors.toList());

        // 유사도 점수 기준 정렬
        placeDtos.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));

        return placeDtos;
    }

    private List<PlaceSearchResponse.PlaceDto> searchByCategory(String category, Double lat, Double lng) {
        // 카테고리로 주변 장소 검색
        List<PlaceWithDistance> searchResults = placeRepository.findPlacesByCategoryWithinRadius(
                category, lat, lng, defaultSearchRadius);

        List<Long> placeIds = searchResults.stream()
                .map(PlaceWithDistance::getId)
                .collect(Collectors.toList());

        // 한 번에 키워드 정보 조회
        List<Place> placesWithKeywords = placeRepository.findByIdsWithKeywords(placeIds);
        Map<Long, Place> placeMap = placesWithKeywords.stream()
                .collect(Collectors.toMap(Place::getId, place -> place));

        // DTO 변환
        return searchResults.stream()
                .map(placeWithDistance -> {
                    Place place = placeMap.get(placeWithDistance.getId());
                    List<String> keywords = place != null ?
                            place.getKeywords().stream()
                                    .map(pk -> pk.getKeyword().getKeyword())
                                    .collect(Collectors.toList()) :
                            Collections.emptyList();

                    return convertToPlaceDtoFromProjection(placeWithDistance, keywords);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PlaceDetailResponse getPlaceDetail(Long placeId) {
        // 한 번의 쿼리로 모든 연관 데이터 조회 (N+1 해결)
        Place place = placeRepository.findCompleteById(placeId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.PLACE_NOT_FOUND,
                        "장소를 찾을 수 없습니다: " + placeId));

        // 이제 모든 데이터가 이미 로드되어 있어 추가 쿼리 없음
        List<String> keywords = place.getKeywords().stream()
                .map(pk -> pk.getKeyword().getKeyword())
                .collect(Collectors.toList());

        List<PlaceDetailResponse.Menu> menuList = place.getMenus().stream()
                .map(menu -> PlaceDetailResponse.Menu.builder()
                        .name(menu.getMenuName())
                        .price(menu.getPrice())
                        .build())
                .collect(Collectors.toList());

        List<PlaceHours> hours = place.getHours();

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
                .thumbnail(place.getImageUrl())
                .location(locationMap)
                .keywords(keywords)
                .description(place.getDescription())
                .openingHours(openingHours)
                .phone(place.getPhone())
                .menu(menuList)
                .build();
    }

    // 기존 메서드들은 그대로 유지
    private PlaceSearchResponse.PlaceDto convertToPlaceDtoFromProjection(
            PlaceWithDistance placeWithDistance, List<String> keywords) {
        String formattedDistance = formatDistance(placeWithDistance.getDistance());

        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("type", "Point");
        locationMap.put("coordinates", new double[]{
                placeWithDistance.getLongitude(),
                placeWithDistance.getLatitude()
        });

        return PlaceSearchResponse.PlaceDto.builder()
                .id(placeWithDistance.getId())
                .name(placeWithDistance.getName())
                .thumbnail(placeWithDistance.getImageUrl())
                .distance(formattedDistance)
                .momentCount("0")
                .keywords(keywords)
                .location(locationMap)
                .similarityScore(null)
                .build();
    }

    private PlaceSearchResponse.PlaceDto convertToPlaceDto(Place place, Double distance, Double similarityScore, List<String> aiKeywords) {
        String formattedDistance = formatDistance(distance);

        List<String> keywords;
        if (aiKeywords != null && !aiKeywords.isEmpty()) {
            keywords = aiKeywords;
        } else {
            keywords = place.getKeywords().stream()
                    .map(pk -> pk.getKeyword().getKeyword())
                    .collect(Collectors.toList());
        }

        Point location = place.getLocation();
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("type", "Point");
        locationMap.put("coordinates", new double[]{location.getX(), location.getY()});

        return PlaceSearchResponse.PlaceDto.builder()
                .id(place.getId())
                .name(place.getName())
                .thumbnail(place.getImageUrl())
                .distance(formattedDistance)
                .momentCount("0")
                .keywords(keywords)
                .location(locationMap)
                .similarityScore(similarityScore)
                .build();
    }

    private List<PlaceDetailResponse.Schedule> buildDaySchedules(List<PlaceHours> placeHours) {
        String[] dayCodesEn = {"mon", "tue", "wed", "thu", "fri", "sat", "sun"};
        Map<String, Map<Boolean, PlaceHours>> hoursByDayAndType = new HashMap<>();

        for (PlaceHours hour : placeHours) {
            String englishDay = DayOfWeek.getEnglishCodeByKoreanCode(hour.getDayOfWeek());

            if (!hoursByDayAndType.containsKey(englishDay)) {
                hoursByDayAndType.put(englishDay, new HashMap<>());
            }

            hoursByDayAndType.get(englishDay).put(hour.getIsBreakTime(), hour);
        }

        List<PlaceDetailResponse.Schedule> schedules = new ArrayList<>();
        for (String dayCode : dayCodesEn) {
            Map<Boolean, PlaceHours> dayHoursMap = hoursByDayAndType.getOrDefault(dayCode, new HashMap<>());

            PlaceHours regularHours = dayHoursMap.get(false);
            PlaceHours breakHours = dayHoursMap.get(true);

            PlaceDetailResponse.Schedule.ScheduleBuilder builder =
                    PlaceDetailResponse.Schedule.builder().day(dayCode);

            if (regularHours != null && regularHours.getOpenTime() != null && regularHours.getCloseTime() != null) {
                builder.hours(regularHours.getOpenTime() + "~" + regularHours.getCloseTime());
            } else {
                builder.hours(null);
            }

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
            return Math.round(distanceInMeters) + "m";
        } else {
            BigDecimal km = BigDecimal.valueOf(distanceInMeters / 1000.0)
                    .setScale(1, RoundingMode.HALF_UP);
            return km.toString() + "km";
        }
    }

    private String determineBusinessStatus(List<PlaceHours> hours) {
        if (hours == null || hours.isEmpty()) {
            return "영업 여부 확인 필요";
        }

        var koreaZoneId = ZoneId.of("Asia/Seoul");
        var now = ZonedDateTime.now(koreaZoneId);

        var koreanDayOfWeek = switch (now.getDayOfWeek()) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };

        var currentHour = now.getHour();
        var currentMinute = now.getMinute();

        var todayRegularHours = hours.stream()
                .filter(h -> h.getDayOfWeek().equals(koreanDayOfWeek) && !h.getIsBreakTime())
                .findFirst();

        var todayBreakHours = hours.stream()
                .filter(h -> h.getDayOfWeek().equals(koreanDayOfWeek) && h.getIsBreakTime())
                .findFirst();

        if (todayRegularHours.isEmpty()) {
            return "영업 정보 없음";
        }

        var regular = todayRegularHours.get();

        if (regular.getOpenTime() == null || regular.getCloseTime() == null) {
            return "휴무일";
        }

        var currentTimeInMinutes = currentHour * 60 + currentMinute;
        var regularOpenTimeInMinutes = parseTimeToMinutes(regular.getOpenTime());
        var regularCloseTimeInMinutes = parseTimeToMinutes(regular.getCloseTime());

        record BreakTime(int start, int end) {}
        var breakTime = todayBreakHours
                .filter(b -> b.getOpenTime() != null && b.getCloseTime() != null)
                .map(b -> new BreakTime(
                        parseTimeToMinutes(b.getOpenTime()),
                        parseTimeToMinutes(b.getCloseTime())
                ));

        if (breakTime.isPresent() &&
                currentTimeInMinutes >= breakTime.get().start &&
                currentTimeInMinutes < breakTime.get().end) {
            return "브레이크 타임";
        }

        return regularOpenTimeInMinutes < regularCloseTimeInMinutes
                ? (currentTimeInMinutes >= regularOpenTimeInMinutes &&
                currentTimeInMinutes < regularCloseTimeInMinutes)
                ? "영업 중" : "영업 종료"
                : (currentTimeInMinutes >= regularOpenTimeInMinutes ||
                currentTimeInMinutes < regularCloseTimeInMinutes)
                ? "영업 중" : "영업 종료";
    }

    private int parseTimeToMinutes(String timeString) {
        try {
            String[] parts = timeString.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            return hour * 60 + minute;
        } catch (Exception e) {
            return 0;
        }
    }
}