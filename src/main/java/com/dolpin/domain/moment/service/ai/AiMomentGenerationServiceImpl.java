package com.dolpin.domain.moment.service.ai;

import com.dolpin.domain.moment.client.MomentAiClient;
import com.dolpin.domain.moment.dto.request.AiMomentGenerationRequest;
import com.dolpin.domain.moment.dto.response.AiMomentGenerationResponse;
import com.dolpin.domain.moment.dto.request.MomentCreateRequest;
import com.dolpin.domain.moment.dto.response.MomentCreateResponse;
import com.dolpin.domain.moment.service.command.MomentCommandService;
import com.dolpin.domain.place.dto.response.PlaceDetailResponse;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.domain.place.service.query.PlaceQueryService;
import com.dolpin.global.constants.SystemUserConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiMomentGenerationServiceImpl implements AiMomentGenerationService {

    private final PlaceQueryService placeQueryService;
    private final PlaceRepository placeRepository;
    private final MomentCommandService momentCommandService;
    private final MomentAiClient momentAiClient;

    @Override
    @Transactional
    public void generateDailyMoment() {
        log.info("Starting daily AI moment generation by Dolpin");

        try {
            generateSingleMomentFromRandomPlace();
            log.info("Daily AI moment generation completed successfully");
        } catch (Exception e) {
            log.error("Failed to generate daily AI moment", e);
            throw e;
        }
    }

    private void generateSingleMomentFromRandomPlace() {
        // 1. 랜덤 장소 선택
        Long randomPlaceId = selectRandomPlace();
        if (randomPlaceId == null) {
            log.warn("No places available for AI moment generation");
            return;
        }

        // 2. 장소 세부 정보 조회
        PlaceDetailResponse placeDetail = placeQueryService.getPlaceDetailWithoutBookmark(randomPlaceId);

        // 3. 장소 정보를 AI에게 전달하여 기록 생성 요청
        AiMomentGenerationRequest aiRequest = buildAiRequest(placeDetail);

        // 4. AI API 호출
        AiMomentGenerationResponse aiResponse = momentAiClient.generateMomentFromPlace(aiRequest);

        if (aiResponse == null) {
            log.warn("AI failed to generate moment for place: {}", placeDetail.getName());
            return;
        }

        log.debug("AI Response - placeId: {}, expected placeId: {}",
                aiResponse.getPlaceId(), randomPlaceId);

        // 5. AI 응답으로 기록 생성
        MomentCreateRequest momentRequest = MomentCreateRequest.builder()
                .title(aiResponse.getTitle())
                .content(aiResponse.getContent())
                .placeId(aiResponse.getPlaceId())
                .placeName(placeDetail.getName())
                .images(aiResponse.getImages())
                .isPublic(aiResponse.getIsPublic())
                .build();

        // 6. 돌핀 유저로 기록 생성
        MomentCreateResponse response = momentCommandService.createMoment(
                SystemUserConstants.DOLPIN_USER_ID,
                momentRequest
        );

        log.info("AI moment created successfully: momentId={}, placeId={}, placeName={}, title={}",
                response.getId(), randomPlaceId, placeDetail.getName(), aiResponse.getTitle());
    }

    private Long selectRandomPlace() {
        List<Long> randomPlaceIds = placeRepository.findRandomPlaceIds(1);

        if (randomPlaceIds.isEmpty()) {
            return null;
        }

        return randomPlaceIds.get(0);
    }

    private AiMomentGenerationRequest buildAiRequest(PlaceDetailResponse placeDetail) {
        return AiMomentGenerationRequest.builder()
                .id(placeDetail.getId())
                .name(placeDetail.getName())
                .address(placeDetail.getAddress())
                .thumbnail(placeDetail.getThumbnail())
                .keyword(placeDetail.getKeywords())
                .description(placeDetail.getDescription())
                .openingHours(convertOpeningHours(placeDetail.getOpeningHours()))
                .phone(placeDetail.getPhone())
                .menu(convertMenu(placeDetail.getMenu()))
                .build();
    }

    private AiMomentGenerationRequest.OpeningHours convertOpeningHours(PlaceDetailResponse.OpeningHours source) {
        if (source == null) return null;

        return AiMomentGenerationRequest.OpeningHours.builder()
                .status(source.getStatus())
                .schedules(source.getSchedules() != null ?
                        source.getSchedules().stream()
                                .map(schedule -> AiMomentGenerationRequest.Schedule.builder()
                                        .day(schedule.getDay())
                                        .hours(schedule.getHours())
                                        .breakTime(schedule.getBreakTime())
                                        .build())
                                .toList() : null)
                .build();
    }

    private List<AiMomentGenerationRequest.Menu> convertMenu(List<PlaceDetailResponse.Menu> sourceMenu) {
        if (sourceMenu == null) return null;

        return sourceMenu.stream()
                .map(menu -> AiMomentGenerationRequest.Menu.builder()
                        .name(menu.getName())
                        .price(menu.getPrice())
                        .build())
                .toList();
    }
}
