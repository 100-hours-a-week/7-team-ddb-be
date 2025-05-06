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
                placeIds, lat, lng, defaultSearchRadius);

        // DTO 변환
        List<PlaceSearchResponse.PlaceDto> placeDtos = nearbyPlaces.stream()
                .map(placeWithDistance -> {
                    Place place = placeWithDistance.getPlace();
                    Double distance = placeWithDistance.getDistance();
                    Double similarityScore = similarityScores.get(place.getId());

                    return convertToPlaceDto(place, distance, similarityScore);
                })
                .collect(Collectors.toList());

        // 유사도 점수 기준 정렬 (null 값은 이제 걱정할 필요 없음)
        placeDtos.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));

        return placeDtos;
    }

    private List<PlaceSearchResponse.PlaceDto> searchByCategory(String category, Double lat, Double lng) {
        // 카테고리로 주변 장소 전체 검색 (페이징 없이)
        List<PlaceWithDistance> searchResults = placeRepository.findPlacesByCategoryWithinRadius(
                category, lat, lng, defaultSearchRadius);

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

        // 현재 영업 상태 확인 (단순 체크에서 실제 영업 상태 판단으로 변경)
        String status = determineBusinessStatus(place.getHours());

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
        String dayOfWeek = hours.getDayOfWeek();
        // 한글 요일을 영어 코드로 변환
        String day = DayOfWeek.getEnglishCodeByKoreanCode(dayOfWeek);

        PlaceDetailResponse.Schedule.ScheduleBuilder builder = PlaceDetailResponse.Schedule.builder()
                .day(day);

        if (hours.getOpenTime() != null && hours.getCloseTime() != null) {
            String hoursString = hours.getOpenTime() + "~" + hours.getCloseTime();
            builder.hours(hoursString);
        } else {
            // 영어 코드를 한글 요일로 변환
            String koreanDay = DayOfWeek.getKoreanCodeByEnglishCode(day);
            builder.note("정기휴무 (매주 " + koreanDay + "요일)");
        }

        return builder.build();
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