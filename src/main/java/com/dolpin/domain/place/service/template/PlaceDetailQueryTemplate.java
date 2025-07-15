package com.dolpin.domain.place.service.template;

import com.dolpin.domain.place.dto.response.PlaceDetailResponse;
import com.dolpin.domain.place.entity.Place;
import com.dolpin.domain.place.entity.PlaceHours;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.domain.place.service.query.PlaceBookmarkQueryService;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import com.dolpin.global.util.DayOfWeek;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public abstract class PlaceDetailQueryTemplate {

    protected final PlaceRepository placeRepository;
    protected final PlaceBookmarkQueryService bookmarkQueryService;

    /**
     * Template Method - Place 상세 조회의 공통 플로우
     */
    public final PlaceDetailResponse getPlaceDetail(Long placeId, Long userId) {
        log.debug("Place 상세 조회 시작: placeId={}, userId={}", placeId, userId);

        // 1. 기본 Place 정보 조회 (공통)
        Place basicPlace = getBasicPlace(placeId);

        // 2. 상세 정보 수집 (각 구현체에서 정의)
        PlaceDetailContext context = collectDetailInformation(placeId, userId);

        // 3. 응답 생성 (공통)
        PlaceDetailResponse response = buildDetailResponse(basicPlace, context);

        log.debug("Place 상세 조회 완료: placeId={}", placeId);
        return response;
    }

    // ============= Abstract Methods (구현체에서 정의) =============

    /**
     * 상세 정보 수집 - 각 구현체에서 필요한 정보만 조회
     */
    protected abstract PlaceDetailContext collectDetailInformation(Long placeId, Long userId);

    // ============= 공통 메서드들 =============

    /**
     * 기본 Place 정보 조회 (공통)
     */
    protected Place getBasicPlace(Long placeId) {
        return placeRepository.findBasicPlaceById(placeId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.PLACE_NOT_FOUND,
                        "장소를 찾을 수 없습니다: " + placeId));
    }

    /**
     * 키워드 정보 조회
     */
    protected List<String> getKeywords(Long placeId) {
        Place placeWithKeywords = placeRepository.findByIdWithKeywords(placeId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.PLACE_NOT_FOUND,
                        "장소를 찾을 수 없습니다: " + placeId));

        return placeWithKeywords.getKeywords().stream()
                .map(pk -> pk.getKeyword().getKeyword())
                .collect(Collectors.toList());
    }

    /**
     * 메뉴 정보 조회
     */
    protected List<PlaceDetailResponse.Menu> getMenus(Long placeId) {
        Place placeWithMenus = placeRepository.findByIdWithMenus(placeId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.PLACE_NOT_FOUND,
                        "장소를 찾을 수 없습니다: " + placeId));

        return placeWithMenus.getMenus().stream()
                .map(menu -> PlaceDetailResponse.Menu.builder()
                        .name(menu.getMenuName())
                        .price(menu.getPrice())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 운영시간 정보 조회
     */
    protected List<PlaceHours> getHours(Long placeId) {
        Place placeWithHours = placeRepository.findByIdWithHours(placeId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.PLACE_NOT_FOUND,
                        "장소를 찾을 수 없습니다: " + placeId));

        return placeWithHours.getHours();
    }

    /**
     * 북마크 정보 조회
     */
    protected Boolean getBookmarkInfo(Long userId, Long placeId) {
        return userId != null ? bookmarkQueryService.isBookmarked(userId, placeId) : null;
    }

    /**
     * 응답 생성 (공통)
     */
    protected PlaceDetailResponse buildDetailResponse(Place basicPlace, PlaceDetailContext context) {
        // Location 맵 생성
        Point location = basicPlace.getLocation();
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("type", "Point");
        locationMap.put("coordinates", new double[]{location.getX(), location.getY()});

        // 운영시간 정보 생성
        PlaceDetailResponse.OpeningHours openingHours = null;
        if (context.getHours() != null) {
            List<PlaceDetailResponse.Schedule> schedules = buildDaySchedules(context.getHours());
            String status = determineBusinessStatus(context.getHours());

            openingHours = PlaceDetailResponse.OpeningHours.builder()
                    .status(status)
                    .schedules(schedules)
                    .build();
        }

        return PlaceDetailResponse.builder()
                .id(basicPlace.getId())
                .name(basicPlace.getName())
                .address(basicPlace.getRoadAddress())
                .thumbnail(basicPlace.getImageUrl())
                .location(locationMap)
                .keywords(context.getKeywords())
                .description(basicPlace.getDescription())
                .openingHours(openingHours)
                .phone(basicPlace.getPhone())
                .menu(context.getMenus())
                .isBookmarked(context.getIsBookmarked())
                .build();
    }

    /**
     * 요일별 운영시간 스케줄 생성
     */
    private List<PlaceDetailResponse.Schedule> buildDaySchedules(List<PlaceHours> placeHours) {
        String[] dayCodesEn = {"mon", "tue", "wed", "thu", "fri", "sat", "sun"};
        Map<String, Map<Boolean, PlaceHours>> hoursByDayAndType = new HashMap<>();

        // 요일별 및 타입별(운영시간/브레이크타임)로 그룹화
        for (PlaceHours hour : placeHours) {
            String englishDay = DayOfWeek.getEnglishCodeByKoreanCode(hour.getDayOfWeek());

            if (!hoursByDayAndType.containsKey(englishDay)) {
                hoursByDayAndType.put(englishDay, new HashMap<>());
            }

            hoursByDayAndType.get(englishDay).put(hour.getIsBreakTime(), hour);
        }

        // 각 요일별 스케줄 생성
        List<PlaceDetailResponse.Schedule> schedules = new ArrayList<>();
        for (String dayCode : dayCodesEn) {
            Map<Boolean, PlaceHours> dayHoursMap = hoursByDayAndType.getOrDefault(dayCode, new HashMap<>());
            PlaceHours regularHours = dayHoursMap.get(false);  // 일반 운영시간
            PlaceHours breakHours = dayHoursMap.get(true);     // 브레이크타임

            PlaceDetailResponse.Schedule.ScheduleBuilder builder =
                    PlaceDetailResponse.Schedule.builder().day(dayCode);

            // 일반 운영시간 설정
            if (regularHours != null && regularHours.getOpenTime() != null && regularHours.getCloseTime() != null) {
                builder.hours(regularHours.getOpenTime() + "~" + regularHours.getCloseTime());
            } else {
                builder.hours(null);
            }

            // 브레이크타임 설정
            if (breakHours != null && breakHours.getOpenTime() != null && breakHours.getCloseTime() != null) {
                builder.breakTime(breakHours.getOpenTime() + "~" + breakHours.getCloseTime());
            } else {
                builder.breakTime(null);
            }

            schedules.add(builder.build());
        }

        return schedules;
    }

    /**
     * 현재 시간 기준 영업 상태 판단
     */
    private String determineBusinessStatus(List<PlaceHours> hours) {
        if (hours == null || hours.isEmpty()) {
            return "영업 여부 확인 필요";
        }

        ZoneId koreaZoneId = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(koreaZoneId);

        // 현재 요일을 한국어 코드로 변환
        String koreanDayOfWeek = switch (now.getDayOfWeek()) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };

        // 현재 요일의 운영시간 찾기
        PlaceHours todayRegularHours = hours.stream()
                .filter(hour -> koreanDayOfWeek.equals(hour.getDayOfWeek()) && !hour.getIsBreakTime())
                .findFirst()
                .orElse(null);

        PlaceHours todayBreakHours = hours.stream()
                .filter(hour -> koreanDayOfWeek.equals(hour.getDayOfWeek()) && hour.getIsBreakTime())
                .findFirst()
                .orElse(null);

        // 운영시간이 없으면 휴무
        if (todayRegularHours == null ||
                todayRegularHours.getOpenTime() == null ||
                todayRegularHours.getCloseTime() == null) {
            return "휴무";
        }

        LocalTime currentTime = now.toLocalTime();
        LocalTime openTime = LocalTime.parse(todayRegularHours.getOpenTime());
        LocalTime closeTime = LocalTime.parse(todayRegularHours.getCloseTime());

        // 24시간 운영 처리 (자정을 넘어가는 경우)
        boolean isOverMidnight = closeTime.isBefore(openTime);

        boolean isOpen;
        if (isOverMidnight) {
            // 예: 22:00 ~ 06:00 (자정을 넘어가는 경우)
            isOpen = currentTime.isAfter(openTime) || currentTime.isBefore(closeTime);
        } else {
            // 일반적인 경우: 09:00 ~ 18:00
            isOpen = currentTime.isAfter(openTime) && currentTime.isBefore(closeTime);
        }

        if (!isOpen) {
            return "휴무";
        }

        // 브레이크타임 확인
        if (todayBreakHours != null &&
                todayBreakHours.getOpenTime() != null &&
                todayBreakHours.getCloseTime() != null) {

            LocalTime breakStart = LocalTime.parse(todayBreakHours.getOpenTime());
            LocalTime breakEnd = LocalTime.parse(todayBreakHours.getCloseTime());

            if (currentTime.isAfter(breakStart) && currentTime.isBefore(breakEnd)) {
                return "브레이크타임";
            }
        }

        return "영업중";
    }
}
