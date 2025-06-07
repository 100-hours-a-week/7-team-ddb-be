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

    // 기존 메서드 (수정 없음)
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

        // 검색어가 있는 경우 새로운 로직 적용
        if (query != null && !query.trim().isEmpty()) {
            placeDtos = searchByQueryNew(query, lat, lng);
        }
        // 카테고리만 있는 경우 기존 로직 유지
        else {
            placeDtos = searchByCategory(category, lat, lng);
        }

        return PlaceSearchResponse.builder()
                .total(placeDtos.size())
                .places(placeDtos)
                .build();
    }

    // 새로 추가된 dev 전용 메서드
    @Override
    @Transactional(readOnly = true)
    public PlaceSearchResponse searchPlacesWithDevToken(String query, Double lat, Double lng, String category, String devToken) {
        log.info(" DEV: Searching with token bypass - query: {}, token: {}",
                query, devToken != null ? "provided" : "null");

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

        // 검색어가 있는 경우 토큰과 함께 AI 호출
        if (query != null && !query.trim().isEmpty()) {
            placeDtos = searchByQueryWithDevToken(query, lat, lng, devToken);
        }
        // 카테고리만 있는 경우 기존 로직 (AI 호출 없음)
        else {
            placeDtos = searchByCategory(category, lat, lng);
        }

        return PlaceSearchResponse.builder()
                .total(placeDtos.size())
                .places(placeDtos)
                .build();
    }

    // dev 토큰과 함께 AI 호출하는 새로운 메서드
    private List<PlaceSearchResponse.PlaceDto> searchByQueryWithDevToken(String query, Double lat, Double lng, String devToken) {
        // 1. 이름 기반 DB 검색
        List<Long> dbSearchIds = placeRepository.findPlaceIdsByNameContaining(query);
        log.info("DB search found {} places for query: {}", dbSearchIds.size(), query);

        // 2. AI 서비스 호출 (토큰과 함께)
        PlaceAiResponse aiResponse = placeAiClient.recommendPlaces(query, devToken);
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

        log.info(" DEV: AI service found {} places with token bypass, suggested category: {}",
                aiRecommendedIds.size(), aiResponse != null ? aiResponse.getPlaceCategory() : null);

        // DB 검색 결과 ID 집합 생성 (중복 체크용)
        Set<Long> dbSearchIdSet = new HashSet<>(dbSearchIds);

        // 3. 결과 병합 (DB 검색 결과를 먼저, AI 결과를 나중에)
        LinkedHashSet<Long> mergedIds = new LinkedHashSet<>();
        mergedIds.addAll(dbSearchIds); // 1번 리스트 먼저
        mergedIds.addAll(aiRecommendedIds); // 2번 리스트 나중에 (중복 자동 제거)

        List<Long> finalIds = new ArrayList<>(mergedIds);

        // 4. 카테고리 필터링 (AI가 카테고리를 제안한 경우)
        if (aiResponse != null && StringUtils.isNotBlank(aiResponse.getPlaceCategory())) {
            finalIds = filterByCategory(finalIds, aiResponse.getPlaceCategory());
            log.info("Filtered by category '{}', remaining {} places",
                    aiResponse.getPlaceCategory(), finalIds.size());
        }

        // 5. 위치 기반 필터링 및 거리 계산
        List<PlaceSearchResponse.PlaceDto> result = new ArrayList<>();
        if (!finalIds.isEmpty()) {
            result = processPlaceIds(finalIds, lat, lng, similarityScores, keywordsByPlaceId, dbSearchIdSet);
        }

        // 6. 폴백: 결과가 비어있고 AI가 카테고리를 제안했다면 해당 카테고리로 검색
        if (result.isEmpty() && aiResponse != null && StringUtils.isNotBlank(aiResponse.getPlaceCategory())) {
            log.info("No results found, falling back to category search: {}", aiResponse.getPlaceCategory());
            result = searchByCategory(aiResponse.getPlaceCategory(), lat, lng);
        }

        return result;
    }

    private List<PlaceSearchResponse.PlaceDto> searchByQueryNew(String query, Double lat, Double lng) {
        // 1. 이름 기반 DB 검색
        List<Long> dbSearchIds = placeRepository.findPlaceIdsByNameContaining(query);
        log.info("DB search found {} places for query: {}", dbSearchIds.size(), query);

        // 2. AI 서비스 호출
        PlaceAiResponse aiResponse = placeAiClient.recommendPlaces(query);
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

        log.info("AI service found {} places, suggested category: {}",
                aiRecommendedIds.size(), aiResponse != null ? aiResponse.getPlaceCategory() : null);

        // DB 검색 결과 ID 집합 생성 (중복 체크용)
        Set<Long> dbSearchIdSet = new HashSet<>(dbSearchIds);

        // 3. 결과 병합 (DB 검색 결과를 먼저, AI 결과를 나중에)
        LinkedHashSet<Long> mergedIds = new LinkedHashSet<>();
        mergedIds.addAll(dbSearchIds); // 1번 리스트 먼저
        mergedIds.addAll(aiRecommendedIds); // 2번 리스트 나중에 (중복 자동 제거)

        List<Long> finalIds = new ArrayList<>(mergedIds);

        // 4. 카테고리 필터링 (AI가 카테고리를 제안한 경우)
        if (aiResponse != null && StringUtils.isNotBlank(aiResponse.getPlaceCategory())) {
            finalIds = filterByCategory(finalIds, aiResponse.getPlaceCategory());
            log.info("Filtered by category '{}', remaining {} places",
                    aiResponse.getPlaceCategory(), finalIds.size());
        }

        // 5. 위치 기반 필터링 및 거리 계산
        List<PlaceSearchResponse.PlaceDto> result = new ArrayList<>();
        if (!finalIds.isEmpty()) {
            result = processPlaceIds(finalIds, lat, lng, similarityScores, keywordsByPlaceId, dbSearchIdSet);
        }

        // 6. 폴백: 결과가 비어있고 AI가 카테고리를 제안했다면 해당 카테고리로 검색
        if (result.isEmpty() && aiResponse != null && StringUtils.isNotBlank(aiResponse.getPlaceCategory())) {
            log.info("No results found, falling back to category search: {}", aiResponse.getPlaceCategory());
            result = searchByCategory(aiResponse.getPlaceCategory(), lat, lng);
        }

        return result;
    }

    private List<Long> filterByCategory(List<Long> placeIds, String category) {
        if (placeIds.isEmpty()) {
            return placeIds;
        }

        // 장소 ID들의 카테고리를 조회해서 필터링
        return placeRepository.findAllById(placeIds).stream()
                .filter(place -> category.equals(place.getCategory()))
                .map(Place::getId)
                .collect(Collectors.toList());
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
            Set<Long> dbSearchIdSet) {

        // PostGIS를 활용한 위치 기반 필터링 및 거리 계산
        List<PlaceWithDistance> nearbyPlaces = placeRepository.findPlacesWithinRadiusByIds(
                placeIds, lat, lng, defaultSearchRadius);

        if (nearbyPlaces.isEmpty()) {
            log.info("No places found within radius");
            return Collections.emptyList();
        }

        // 필터링된 장소 ID 목록
        List<Long> filteredPlaceIds = nearbyPlaces.stream()
                .map(PlaceWithDistance::getId)
                .collect(Collectors.toList());

        // Moment 개수를 한 번에 조회
        Map<Long, Long> momentCountMap = getMomentCountMap(filteredPlaceIds);

        // 거리 정보 맵 구성
        Map<Long, Double> distanceMap = nearbyPlaces.stream()
                .collect(Collectors.toMap(
                        PlaceWithDistance::getId,
                        PlaceWithDistance::getDistance
                ));

        // 키워드를 포함한 장소 정보 한 번에 조회 (N+1 문제 방지)
        List<Place> placesWithKeywords = placeRepository.findByIdsWithKeywords(filteredPlaceIds);

        // 원래 순서 유지를 위한 맵 생성
        Map<Long, Place> placeMap = placesWithKeywords.stream()
                .collect(Collectors.toMap(Place::getId, place -> place));

        // DB 검색 결과와 AI 추천 결과 분리하여 처리
        List<PlaceSearchResponse.PlaceDto> dbResults = new ArrayList<>();
        List<PlaceSearchResponse.PlaceDto> aiResults = new ArrayList<>();

        for (Long placeId : placeIds) {
            if (!placeMap.containsKey(placeId)) continue; // 반경 내에 없는 장소 제외

            Place place = placeMap.get(placeId);
            Double distance = distanceMap.get(placeId);
            Double similarityScore = similarityScores.get(placeId);

            // AI에서 제공된 키워드 리스트 가져오기
            List<String> keywords = keywordsByPlaceId.getOrDefault(placeId,
                    place.getKeywords().stream()
                            .map(pk -> pk.getKeyword().getKeyword())
                            .collect(Collectors.toList())
            );

            PlaceSearchResponse.PlaceDto dto = convertToPlaceDto(place, distance, similarityScore, keywords, momentCountMap);

            // DB 검색 결과인지 확인 (dbSearchIdSet에 있으면 DB 결과)
            if (dbSearchIdSet.contains(placeId)) {
                dbResults.add(dto);
            } else {
                aiResults.add(dto);
            }
        }

        // AI 결과만 similarity_score 기준으로 정렬 (내림차순)
        aiResults.sort((a, b) -> {
            if (a.getSimilarityScore() == null && b.getSimilarityScore() == null) return 0;
            if (a.getSimilarityScore() == null) return 1;
            if (b.getSimilarityScore() == null) return -1;
            return Double.compare(b.getSimilarityScore(), a.getSimilarityScore());
        });

        // DB 결과를 앞에, AI 결과를 뒤에 배치
        List<PlaceSearchResponse.PlaceDto> finalResults = new ArrayList<>();
        finalResults.addAll(dbResults);
        finalResults.addAll(aiResults);

        return finalResults;
    }

    private List<PlaceSearchResponse.PlaceDto> searchByCategory(String category, Double lat, Double lng) {
        // 카테고리로 주변 장소 전체 검색
        List<PlaceWithDistance> searchResults = placeRepository.findPlacesByCategoryWithinRadius(
                category, lat, lng, defaultSearchRadius);

        // 장소 ID 목록 추출
        List<Long> placeIds = searchResults.stream()
                .map(PlaceWithDistance::getId)
                .collect(Collectors.toList());

        // Moment 개수를 한 번에 조회
        Map<Long, Long> momentCountMap = getMomentCountMap(placeIds);

        // 키워드를 포함한 장소 정보 조회
        List<Place> placesWithKeywords = placeRepository.findByIdsWithKeywords(placeIds);

        // Place ID -> Place 매핑
        Map<Long, Place> placeMap = placesWithKeywords.stream()
                .collect(Collectors.toMap(Place::getId, place -> place));

        // DTO 변환
        return searchResults.stream()
                .map(placeWithDistance -> {
                    Place place = placeMap.get(placeWithDistance.getId());
                    if (place != null) {
                        // DB 키워드 사용
                        List<String> keywords = place.getKeywords().stream()
                                .map(pk -> pk.getKeyword().getKeyword())
                                .collect(Collectors.toList());

                        return convertToPlaceDtoFromProjection(placeWithDistance, keywords, momentCountMap);
                    } else {
                        // Place 정보가 없는 경우 빈 키워드로 처리
                        return convertToPlaceDtoFromProjection(placeWithDistance, Collections.emptyList(), momentCountMap);
                    }
                })
                .collect(Collectors.toList());
    }

    private PlaceSearchResponse.PlaceDto convertToPlaceDtoFromProjection(
            PlaceWithDistance placeWithDistance, List<String> keywords, Map<Long, Long> momentCountMap) {
        // 거리 포맷팅
        Double convertedDistance = convertDistance(placeWithDistance.getDistance());

        // Moment 개수 조회 (Map에서)
        Long momentCount = momentCountMap.getOrDefault(placeWithDistance.getId(), 0L);

        // 실제 장소의 위치 정보 사용
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("type", "Point");
        locationMap.put("coordinates", new double[]{
                placeWithDistance.getLongitude(),
                placeWithDistance.getLatitude()
        });

        // DTO 생성 - 키워드 포함
        return PlaceSearchResponse.PlaceDto.builder()
                .id(placeWithDistance.getId())
                .name(placeWithDistance.getName())
                .thumbnail(placeWithDistance.getImageUrl())
                .distance(convertedDistance)
                .momentCount(momentCount)
                .keywords(keywords)
                .location(locationMap)
                .similarityScore(null)
                .build();
    }

    private PlaceSearchResponse.PlaceDto convertToPlaceDto(Place place, Double distance, Double similarityScore,
                                                           List<String> aiKeywords, Map<Long, Long> momentCountMap) {
        // 거리 포맷팅
        Double convertedDistance = convertDistance(distance);

        // Moment 개수 조회 (Map에서)
        Long momentCount = momentCountMap.getOrDefault(place.getId(), 0L);

        // 키워드 추출
        List<String> keywords;
        if (aiKeywords != null && !aiKeywords.isEmpty()) {
            // AI 제공 키워드 사용
            keywords = aiKeywords;
        } else {
            // DB 키워드 사용
            keywords = place.getKeywords().stream()
                    .map(pk -> pk.getKeyword().getKeyword())
                    .collect(Collectors.toList());
        }

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
                .distance(convertedDistance)
                .momentCount(momentCount)
                .keywords(keywords)
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
                .thumbnail(place.getImageUrl())
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

    private Double convertDistance(Double distanceInMeters) {
        if (distanceInMeters == null) return 0.0;

        if (distanceInMeters < 1000) {
            // 미터 단위 - 반올림해서 반환
            return (double) Math.round(distanceInMeters);
        } else {
            // 킬로미터 단위 - 소수점 1자리까지
            return BigDecimal.valueOf(distanceInMeters / 1000.0)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }
    }

    private String determineBusinessStatus(List<PlaceHours> hours) {
        if (hours == null || hours.isEmpty()) {
            return "영업 여부 확인 필요";
        }

        // 한국 시간대 기준으로 현재 시간 가져오기
        var koreaZoneId = ZoneId.of("Asia/Seoul");
        var now = ZonedDateTime.now(koreaZoneId);

        // 현재 요일 (한글)
        var koreanDayOfWeek = switch (now.getDayOfWeek()) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };

        // 현재 시간 정보
        var currentHour = now.getHour();
        var currentMinute = now.getMinute();

        // 오늘의 영업 시간 및 브레이크 타임 찾기 (Java 17의 stream API 활용)
        var todayRegularHours = hours.stream()
                .filter(h -> h.getDayOfWeek().equals(koreanDayOfWeek) && !h.getIsBreakTime())
                .findFirst();

        var todayBreakHours = hours.stream()
                .filter(h -> h.getDayOfWeek().equals(koreanDayOfWeek) && h.getIsBreakTime())
                .findFirst();

        // 영업 정보가 없는 경우
        if (todayRegularHours.isEmpty()) {
            return "영업 정보 없음";
        }

        var regular = todayRegularHours.get();

        // 휴무일인 경우
        if (regular.getOpenTime() == null || regular.getCloseTime() == null) {
            return "휴무일";
        }

        // 시간을 분 단위로 변환
        var currentTimeInMinutes = currentHour * 60 + currentMinute;
        var regularOpenTimeInMinutes = parseTimeToMinutes(regular.getOpenTime());
        var regularCloseTimeInMinutes = parseTimeToMinutes(regular.getCloseTime());

        // 브레이크 타임 정보 (Optional 활용)
        record BreakTime(int start, int end) {}
        var breakTime = todayBreakHours
                .filter(b -> b.getOpenTime() != null && b.getCloseTime() != null)
                .map(b -> new BreakTime(
                        parseTimeToMinutes(b.getOpenTime()),
                        parseTimeToMinutes(b.getCloseTime())
                ));

        // 브레이크 타임 체크
        if (breakTime.isPresent() &&
                currentTimeInMinutes >= breakTime.get().start &&
                currentTimeInMinutes < breakTime.get().end) {
            return "브레이크 타임";
        }

        // 영업 상태 확인 (삼항 연산자로 간결하게)
        return regularOpenTimeInMinutes < regularCloseTimeInMinutes
                ? (currentTimeInMinutes >= regularOpenTimeInMinutes &&
                currentTimeInMinutes < regularCloseTimeInMinutes)
                ? "영업 중" : "영업 종료"
                : (currentTimeInMinutes >= regularOpenTimeInMinutes ||
                currentTimeInMinutes < regularCloseTimeInMinutes)
                ? "영업 중" : "영업 종료";
    }

    // 시간 문자열을 분 단위로 변환하는 헬퍼 메서드
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
