package com.dolpin.domain.place.service.template;

import com.dolpin.domain.place.dto.response.PlaceBusinessStatusResponse;
import com.dolpin.domain.place.entity.Place;
import com.dolpin.domain.place.entity.PlaceHours;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public abstract class BusinessStatusQueryTemplate {

    protected final PlaceRepository placeRepository;

    /**
     * Template Method - 영업 상태 조회의 공통 플로우
     */
    public final PlaceBusinessStatusResponse getBusinessStatus(Long placeId) {
        log.debug("영업 상태 조회 시작: placeId={}", placeId);

        // 1. 기본 Place 정보 조회 (공통)
        Place basicPlace = getBasicPlace(placeId);

        // 2. 운영시간 정보 수집 (각 구현체에서 정의)
        BusinessStatusContext context = collectBusinessStatusInformation(placeId);

        // 3. 응답 생성 (공통)
        PlaceBusinessStatusResponse response = buildBusinessStatusResponse(basicPlace, context);

        log.debug("영업 상태 조회 완료: placeId={}, status={}", placeId, response.getStatus());
        return response;
    }

    // ============= Abstract Methods (구현체에서 정의) =============

    /**
     * 영업 상태 판단에 필요한 정보 수집 - 각 구현체에서 정의
     */
    protected abstract BusinessStatusContext collectBusinessStatusInformation(Long placeId);

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
     * 운영시간 정보 조회
     */
    protected List<PlaceHours> getHours(Long placeId) {
        Place placeWithHours = placeRepository.findByIdWithHours(placeId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.PLACE_NOT_FOUND,
                        "장소를 찾을 수 없습니다: " + placeId));

        return placeWithHours.getHours();
    }

    /**
     * 응답 생성 (공통)
     */
    protected PlaceBusinessStatusResponse buildBusinessStatusResponse(Place basicPlace, BusinessStatusContext context) {
        String status = determineBusinessStatus(context.getHours());

        return PlaceBusinessStatusResponse.builder()
                .placeId(basicPlace.getId())
                .status(status)
                .build();
    }

    /**
     * 현재 시간 기준 영업 상태 판단 (기존 로직 활용)
     */
    private String determineBusinessStatus(List<PlaceHours> hours) {
        if (hours == null || hours.isEmpty()) {
            return "영업 여부 확인 필요";
        }

        ZoneId koreaZoneId = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(koreaZoneId);

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
            isOpen = currentTime.isAfter(openTime) || currentTime.isBefore(closeTime);
        } else {
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
