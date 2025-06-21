package com.dolpin.domain.place.service.query;

import com.dolpin.domain.moment.repository.MomentRepository;
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
import java.time.Duration;
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
    private final MomentRepository momentRepository;
    private final PlaceBookmarkQueryService bookmarkQueryService;

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
    public PlaceSearchResponse searchPlaces(String query, Double lat, Double lng, String category, Long userId) {
        return executeSearchLogic(query, lat, lng, category, userId, null);
    }

    @Override
    public PlaceSearchResponse searchPlacesWithDevToken(String query, Double lat, Double lng, String category, String devToken, Long userId) {
        return executeSearchLogic(query, lat, lng, category, userId, devToken);
    }

    private PlaceSearchResponse executeSearchLogic(String query, Double lat, Double lng, String category, Long userId, String devToken) {
        boolean hasQuery = StringUtils.isNotBlank(query);
        boolean hasCategory = StringUtils.isNotBlank(category);

        // 검증 로직
        if (hasQuery && hasCategory) {
            throw new BusinessException(
                    ResponseStatus.INVALID_PARAMETER,
                    "검색어와 카테고리 중 하나만 선택해주세요");
        }

        if (!hasQuery && !hasCategory) {
            throw new BusinessException(
                    ResponseStatus.INVALID_PARAMETER,
                    "검색어 또는 카테고리가 필요합니다");
        }

        if (lat == null || lng == null) {
            throw new BusinessException(
                    ResponseStatus.INVALID_PARAMETER,
                    "위치 정보가 필요합니다");
        }

        List<PlaceSearchResponse.PlaceDto> placeDtos;

        if (query != null && !query.trim().isEmpty()) {
            placeDtos = searchByQueryWithTransactionSeparation(query, lat, lng, devToken, userId);
        } else {
            placeDtos = searchByCategoryWithTransaction(category, lat, lng, userId);
        }

        return PlaceSearchResponse.builder()
                .total(placeDtos.size())
                .places(placeDtos)
                .build();
    }

    private List<PlaceSearchResponse.PlaceDto> searchByQueryWithTransactionSeparation(String query, Double lat, Double lng, String devToken, Long userId) {
        List<Long> dbSearchIds = executeDbSearch(query);

        PlaceAiResponse aiResponse = null;
        try {
            aiResponse = devToken != null
                    ? executeAiSearchWithDevToken(query, devToken)
                    : executeAiSearch(query);
        } catch (Exception e) {
            // AI 실패는 로깅만 하고 계속 진행 (글로벌 핸들러가 이미 처리함)
            log.warn("AI 검색 실패, db검색만 반환. Query: {}", query);
        }

        return processSearchResults(dbSearchIds, aiResponse, lat, lng, userId);
    }

    // 1단계: DB 검색만 (짧은 트랜잭션)
    @Transactional(readOnly = true)
    protected List<Long> executeDbSearch(String query) {
        List<Long> dbSearchIds = placeRepository.findPlaceIdsByNameContaining(query);
        log.info("DB에서 찾은 장소 {} 쿼리: {}", dbSearchIds.size(), query);
        return dbSearchIds;
    }

    private PlaceAiResponse executeAiSearch(String query) {
        PlaceAiResponse aiResponse = placeAiClient.recommendPlacesAsync(query)
                .timeout(Duration.ofSeconds(30))
                .block();

        log.info("AI service found {} places, suggested category: {}",
                aiResponse != null && aiResponse.getRecommendations() != null ?
                        aiResponse.getRecommendations().size() : 0,
                aiResponse != null ? aiResponse.getPlaceCategory() : null);
        return aiResponse;
    }

    private PlaceAiResponse executeAiSearchWithDevToken(String query, String devToken) {
        PlaceAiResponse aiResponse = placeAiClient.recommendPlacesAsync(query, devToken)
                .timeout(Duration.ofSeconds(30))
                .block();

        log.info("DEV: AI service found {} places with token bypass, suggested category: {}",
                aiResponse != null && aiResponse.getRecommendations() != null ?
                        aiResponse.getRecommendations().size() : 0,
                aiResponse != null ? aiResponse.getPlaceCategory() : null);
        return aiResponse;
    }

    // 3단계: 결과 처리
    @Transactional(readOnly = true)
    protected List<PlaceSearchResponse.PlaceDto> processSearchResults(List<Long> dbSearchIds, PlaceAiResponse aiResponse, Double lat, Double lng, Long userId) {
        // 기존 로직 그대로 사용하되 트랜잭션 내에서 실행
        List<Long> aiRecommendedIds = new ArrayList<>();
        Map<Long, Double> similarityScores = new HashMap<>();
        Map<Long, List<String>> keywordsByPlaceId = new HashMap<>();

        if (aiResponse != null && aiResponse.getRecommendations() != null) {
            for (PlaceAiResponse.PlaceRecommendation rec : aiResponse.getRecommendations()) {
                aiRecommendedIds.add(rec.getId());
                similarityScores.put(rec.getId(), rec.getSimilarityScore());
                if (rec.getKeyword() != null && !rec.getKeyword().isEmpty()) {
                    keywordsByPlaceId.put(rec.getId(), rec.getKeyword());
                }
            }
        }

        Set<Long> dbSearchIdSet = new HashSet<>(dbSearchIds);

        // 결과 병합
        LinkedHashSet<Long> mergedIds = new LinkedHashSet<>();
        mergedIds.addAll(dbSearchIds);
        mergedIds.addAll(aiRecommendedIds);
        List<Long> finalIds = new ArrayList<>(mergedIds);

        // 카테고리 필터링
        if (aiResponse != null && StringUtils.isNotBlank(aiResponse.getPlaceCategory())) {
            finalIds = filterByCategoryInTransaction(finalIds, aiResponse.getPlaceCategory());
            log.info("Filtered by category '{}', remaining {} places",
                    aiResponse.getPlaceCategory(), finalIds.size());
        }

        // 위치 기반 처리
        List<PlaceSearchResponse.PlaceDto> result = new ArrayList<>();
        if (!finalIds.isEmpty()) {
            result = processPlaceIds(finalIds, lat, lng, similarityScores, keywordsByPlaceId, dbSearchIdSet, userId);
        }

        // 폴백 처리
        if (result.isEmpty() && aiResponse != null && StringUtils.isNotBlank(aiResponse.getPlaceCategory())) {
            log.info("No results found, falling back to category search: {}", aiResponse.getPlaceCategory());
            result = searchByCategory(aiResponse.getPlaceCategory(), lat, lng, userId);
        }

        return result;
    }

    // 카테고리 검색
    @Transactional(readOnly = true)
    protected List<PlaceSearchResponse.PlaceDto> searchByCategoryWithTransaction(String category, Double lat, Double lng, Long userId) {
        return searchByCategory(category, lat, lng, userId);
    }

    // 카테고리 필터링
    private List<Long> filterByCategoryInTransaction(List<Long> placeIds, String category) {
        if (placeIds.isEmpty()) {
            return placeIds;
        }

        // 장소 ID들의 카테고리를 조회해서 필터링
        return placeRepository.findAllById(placeIds).stream()
                .filter(place -> category.equals(place.getCategory()))
                .map(Place::getId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PlaceDetailResponse getPlaceDetail(Long placeId, Long userId) {
        return getPlaceDetailInternal(placeId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public PlaceDetailResponse getPlaceDetailWithoutBookmark(Long placeId) {
        return getPlaceDetailInternal(placeId, null);
    }

    private PlaceDetailResponse getPlaceDetailInternal(Long placeId, Long userId) {
        Place place = placeRepository.findBasicPlaceById(placeId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.PLACE_NOT_FOUND,
                        "장소를 찾을 수 없습니다: " + placeId));

        Place placeWithKeywords = placeRepository.findByIdWithKeywords(placeId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.PLACE_NOT_FOUND,
                        "장소를 찾을 수 없습니다: " + placeId));
        List<String> keywords = placeWithKeywords.getKeywords().stream()
                .map(pk -> pk.getKeyword().getKeyword())
                .collect(Collectors.toList());

        Place placeWithMenus = placeRepository.findByIdWithMenus(placeId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.PLACE_NOT_FOUND,
                        "장소를 찾을 수 없습니다: " + placeId));
        List<PlaceDetailResponse.Menu> menuList = placeWithMenus.getMenus().stream()
                .map(menu -> PlaceDetailResponse.Menu.builder()
                        .name(menu.getMenuName())
                        .price(menu.getPrice())
                        .build())
                .collect(Collectors.toList());

        Place placeWithHours = placeRepository.findByIdWithHours(placeId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.PLACE_NOT_FOUND,
                        "장소를 찾을 수 없습니다: " + placeId));
        List<PlaceHours> hours = placeWithHours.getHours();

        Point location = place.getLocation();
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("type", "Point");
        locationMap.put("coordinates", new double[]{location.getX(), location.getY()});

        List<PlaceDetailResponse.Schedule> schedules = buildDaySchedules(hours);
        String status = determineBusinessStatus(hours);

        PlaceDetailResponse.OpeningHours openingHours = PlaceDetailResponse.OpeningHours.builder()
                .status(status)
                .schedules(schedules)
                .build();

        // 북마크 여부 확인 (userId가 있을 때만)
        Boolean isBookmarked = userId != null ? bookmarkQueryService.isBookmarked(userId, placeId) : null;

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
                .isBookmarked(isBookmarked)
                .build();
    }

    private Map<Long, Long> getMomentCountMap(List<Long> placeIds) {
        if (placeIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Object[]> results = momentRepository.countPublicMomentsByPlaceIds(placeIds);
        Map<Long, Long> momentCountMap = new HashMap<>();

        for (Object[] result : results) {
            Long placeId = (Long) result[0];
            Long count = (Long) result[1];
            momentCountMap.put(placeId, count);
        }

        return momentCountMap;
    }

    private List<PlaceSearchResponse.PlaceDto> processPlaceIds(
            List<Long> placeIds, Double lat, Double lng,
            Map<Long, Double> similarityScores,
            Map<Long, List<String>> keywordsByPlaceId,
            Set<Long> dbSearchIdSet,
            Long userId) {

        List<PlaceWithDistance> nearbyPlaces = placeRepository.findPlacesWithinRadiusByIds(
                placeIds, lat, lng, defaultSearchRadius);

        if (nearbyPlaces.isEmpty()) {
            log.info("No places found within radius");
            return Collections.emptyList();
        }

        List<Long> filteredPlaceIds = nearbyPlaces.stream()
                .map(PlaceWithDistance::getId)
                .collect(Collectors.toList());

        Map<Long, Long> momentCountMap = getMomentCountMap(filteredPlaceIds);

        // 북마크 상태 일괄 조회
        Map<Long, Boolean> bookmarkStatusMap = bookmarkQueryService.getBookmarkStatusMap(userId, filteredPlaceIds);

        Map<Long, Double> distanceMap = nearbyPlaces.stream()
                .collect(Collectors.toMap(
                        PlaceWithDistance::getId,
                        PlaceWithDistance::getDistance
                ));

        List<Place> placesWithKeywords = placeRepository.findByIdsWithKeywords(filteredPlaceIds);

        Map<Long, Place> placeMap = placesWithKeywords.stream()
                .collect(Collectors.toMap(Place::getId, place -> place));

        List<PlaceSearchResponse.PlaceDto> dbResults = new ArrayList<>();
        List<PlaceSearchResponse.PlaceDto> aiResults = new ArrayList<>();

        for (Long placeId : placeIds) {
            if (!placeMap.containsKey(placeId)) continue;

            Place place = placeMap.get(placeId);
            Double distance = distanceMap.get(placeId);
            Double similarityScore = similarityScores.get(placeId);
            Boolean isBookmarked = bookmarkStatusMap.getOrDefault(placeId, false);

            List<String> keywords = keywordsByPlaceId.getOrDefault(placeId,
                    place.getKeywords().stream()
                            .map(pk -> pk.getKeyword().getKeyword())
                            .collect(Collectors.toList())
            );

            PlaceSearchResponse.PlaceDto dto = convertToPlaceDto(place, distance, similarityScore, keywords, momentCountMap, isBookmarked);

            if (dbSearchIdSet.contains(placeId)) {
                dbResults.add(dto);
            } else {
                aiResults.add(dto);
            }
        }

        aiResults.sort((a, b) -> {
            if (a.getSimilarityScore() == null && b.getSimilarityScore() == null) return 0;
            if (a.getSimilarityScore() == null) return 1;
            if (b.getSimilarityScore() == null) return -1;
            return Double.compare(b.getSimilarityScore(), a.getSimilarityScore());
        });

        List<PlaceSearchResponse.PlaceDto> finalResults = new ArrayList<>();
        finalResults.addAll(dbResults);
        finalResults.addAll(aiResults);

        return finalResults;
    }

    private List<PlaceSearchResponse.PlaceDto> searchByCategory(String category, Double lat, Double lng, Long userId) {
        List<PlaceWithDistance> searchResults = placeRepository.findPlacesByCategoryWithinRadius(
                category, lat, lng, defaultSearchRadius);

        List<Long> placeIds = searchResults.stream()
                .map(PlaceWithDistance::getId)
                .collect(Collectors.toList());

        Map<Long, Long> momentCountMap = getMomentCountMap(placeIds);

        // 북마크 상태 일괄 조회
        Map<Long, Boolean> bookmarkStatusMap = bookmarkQueryService.getBookmarkStatusMap(userId, placeIds);

        List<Place> placesWithKeywords = placeRepository.findByIdsWithKeywords(placeIds);

        Map<Long, Place> placeMap = placesWithKeywords.stream()
                .collect(Collectors.toMap(Place::getId, place -> place));

        return searchResults.stream()
                .map(placeWithDistance -> {
                    Place place = placeMap.get(placeWithDistance.getId());
                    Boolean isBookmarked = bookmarkStatusMap.getOrDefault(placeWithDistance.getId(), false);

                    if (place != null) {
                        List<String> keywords = place.getKeywords().stream()
                                .map(pk -> pk.getKeyword().getKeyword())
                                .collect(Collectors.toList());

                        return convertToPlaceDtoFromProjection(placeWithDistance, keywords, momentCountMap, isBookmarked);
                    } else {
                        return convertToPlaceDtoFromProjection(placeWithDistance, Collections.emptyList(), momentCountMap, isBookmarked);
                    }
                })
                .collect(Collectors.toList());
    }

    private PlaceSearchResponse.PlaceDto convertToPlaceDtoFromProjection(
            PlaceWithDistance placeWithDistance, List<String> keywords, Map<Long, Long> momentCountMap, Boolean isBookmarked) {
        Double convertedDistance = convertDistance(placeWithDistance.getDistance());
        Long momentCount = momentCountMap.getOrDefault(placeWithDistance.getId(), 0L);

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
                .distance(convertedDistance)
                .momentCount(momentCount)
                .keywords(keywords)
                .location(locationMap)
                .isBookmarked(isBookmarked)
                .similarityScore(null)
                .build();
    }

    private PlaceSearchResponse.PlaceDto convertToPlaceDto(Place place, Double distance, Double similarityScore,
                                                           List<String> aiKeywords, Map<Long, Long> momentCountMap, Boolean isBookmarked) {
        Double convertedDistance = convertDistance(distance);
        Long momentCount = momentCountMap.getOrDefault(place.getId(), 0L);

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
                .distance(convertedDistance)
                .momentCount(momentCount)
                .keywords(keywords)
                .location(locationMap)
                .isBookmarked(isBookmarked)
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

    private Double convertDistance(Double distanceInMeters) {
        if (distanceInMeters == null) return 0.0;

        if (distanceInMeters < 1000) {
            return (double) Math.round(distanceInMeters);
        } else {
            return BigDecimal.valueOf(distanceInMeters / 1000.0)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
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
