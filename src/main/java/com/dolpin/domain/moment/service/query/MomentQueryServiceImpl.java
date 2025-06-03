package com.dolpin.domain.moment.service.query;

import com.dolpin.domain.moment.dto.response.MomentDetailResponse;
import com.dolpin.domain.moment.dto.response.MomentListResponse;
import com.dolpin.domain.moment.dto.response.PlaceMomentListResponse;
import com.dolpin.domain.moment.entity.Moment;
import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.user.service.UserQueryService;
import com.dolpin.domain.user.entity.User;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomentQueryServiceImpl implements MomentQueryService {

    private final MomentRepository momentRepository;
    private final UserQueryService userQueryService;

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    @Override
    @Transactional(readOnly = true)
    public MomentListResponse getAllMoments(Integer limit, String cursor) {
        int pageSize = validateAndGetLimit(limit);
        LocalDateTime cursorTime = parseCursor(cursor);

        Pageable pageable = PageRequest.of(0, pageSize + 1); // +1로 hasNext 판단
        Page<Moment> moments = momentRepository.findPublicMoments(pageable);

        return buildMomentListResponse(moments.getContent(), pageSize, true);
    }

    @Override
    @Transactional(readOnly = true)
    public MomentListResponse getMyMoments(Long userId, Integer limit, String cursor) {
        int pageSize = validateAndGetLimit(limit);
        LocalDateTime cursorTime = parseCursor(cursor);

        Pageable pageable = PageRequest.of(0, pageSize + 1);
        Page<Moment> moments = momentRepository.findByUserIdWithVisibility(userId, true, pageable);

        return buildMomentListResponse(moments.getContent(), pageSize, false);
    }

    @Override
    @Transactional(readOnly = true)
    public MomentListResponse getUserMoments(Long targetUserId, Integer limit, String cursor) {
        // 사용자 존재 확인
        userQueryService.getUserById(targetUserId);

        int pageSize = validateAndGetLimit(limit);
        LocalDateTime cursorTime = parseCursor(cursor);

        Pageable pageable = PageRequest.of(0, pageSize + 1);
        Page<Moment> moments = momentRepository.findByUserIdWithVisibility(targetUserId, false, pageable);

        return buildMomentListResponse(moments.getContent(), pageSize, false);
    }

    @Override
    @Transactional(readOnly = true)
    public PlaceMomentListResponse getPlaceMoments(Long placeId) {
        // 전체 조회 (페이징 없음)
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<Moment> moments = momentRepository.findPublicMomentsByPlaceId(placeId, pageable);

        List<PlaceMomentListResponse.PlaceMomentDto> momentDtos = moments.getContent().stream()
                .map(this::buildPlaceMomentDto)
                .collect(Collectors.toList());

        return PlaceMomentListResponse.builder()
                .total(momentDtos.size())
                .moments(momentDtos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MomentDetailResponse getMomentDetail(Long momentId, Long currentUserId) {
        Moment moment = momentRepository.findByIdWithImages(momentId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.USER_NOT_FOUND.withMessage("기록을 찾을 수 없습니다.")));

        // 비공개 기록인 경우 소유자만 조회 가능
        if (!moment.getIsPublic() && (currentUserId == null || !moment.getUserId().equals(currentUserId))) {
            throw new BusinessException(ResponseStatus.FORBIDDEN.withMessage("접근 권한이 없습니다."));
        }

        boolean isOwner = currentUserId != null && moment.getUserId().equals(currentUserId);

        return MomentDetailResponse.from(moment, isOwner);
    }

    private PlaceMomentListResponse.PlaceMomentDto buildPlaceMomentDto(Moment moment) {
        // 첫 번째 이미지를 썸네일로 사용
        String thumbnail = moment.getImages().isEmpty() ? null : moment.getImages().get(0).getImageUrl();

        // 작성자 정보 조회
        User author = userQueryService.getUserById(moment.getUserId());

        return PlaceMomentListResponse.PlaceMomentDto.builder()
                .id(moment.getId())
                .title(moment.getTitle())
                .thumbnail(thumbnail)
                .imagesCount(moment.getImages().size())
                .isPublic(moment.getIsPublic())
                .createdAt(moment.getCreatedAt())
                .place(PlaceMomentListResponse.PlaceDto.builder()
                        .id(moment.getPlaceId())
                        .name(moment.getPlaceName())
                        .build())
                .author(PlaceMomentListResponse.AuthorDto.builder()
                        .id(author.getId())
                        .nickname(author.getUsername())
                        .profileImage(author.getImageUrl())
                        .build())
                .build();
    }

    private MomentListResponse buildMomentListResponse(List<Moment> moments, int pageSize, boolean includeAuthor) {
        boolean hasNext = moments.size() > pageSize;
        List<Moment> actualMoments = hasNext ? moments.subList(0, pageSize) : moments;

        List<MomentListResponse.MomentSummaryDto> momentDtos = actualMoments.stream()
                .map(moment -> buildMomentSummaryDto(moment, includeAuthor))
                .collect(Collectors.toList());

        String nextCursor = null;
        if (hasNext && !actualMoments.isEmpty()) {
            nextCursor = actualMoments.get(actualMoments.size() - 1)
                    .getCreatedAt()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
        }

        MomentListResponse.MetaDto meta = MomentListResponse.MetaDto.builder()
                .pagination(MomentListResponse.PaginationDto.builder()
                        .limit(pageSize)
                        .nextCursor(nextCursor)
                        .hasNext(hasNext)
                        .build())
                .build();

        // TODO: _links 구현
        MomentListResponse.LinksDto links = MomentListResponse.LinksDto.builder()
                .self(MomentListResponse.LinkDto.builder().href("").build())
                .next(hasNext ? MomentListResponse.LinkDto.builder().href("").build() : null)
                .build();

        return MomentListResponse.builder()
                .moments(momentDtos)
                .meta(meta)
                .links(links)
                .build();
    }

    private MomentListResponse.MomentSummaryDto buildMomentSummaryDto(Moment moment, boolean includeAuthor) {
        // 첫 번째 이미지를 썸네일로 사용
        String thumbnail = moment.getImages().isEmpty() ? null : moment.getImages().get(0).getImageUrl();

        MomentListResponse.MomentSummaryDto.MomentSummaryDtoBuilder builder = MomentListResponse.MomentSummaryDto.builder()
                .id(moment.getId())
                .title(moment.getTitle())
                .thumbnail(thumbnail)
                .imagesCount(moment.getImages().size())
                .isPublic(moment.getIsPublic())
                .createdAt(moment.getCreatedAt())
                .place(moment.getPlaceId() != null ?
                        MomentListResponse.PlaceDto.builder()
                                .id(moment.getPlaceId())
                                .name(moment.getPlaceName())
                                .build()
                        : null);

        // 전체 기록 목록 조회시에만 작성자 정보 포함
        if (includeAuthor) {
            User author = userQueryService.getUserById(moment.getUserId());
            builder.author(MomentListResponse.AuthorDto.builder()
                    .id(author.getId())
                    .nickname(author.getUsername())
                    .profileImage(author.getImageUrl())
                    .build());
        }

        return builder.build();
    }

    private int validateAndGetLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private LocalDateTime parseCursor(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return LocalDateTime.now();
        }

        try {
            // ISO 형식의 cursor를 파싱
            String cleanCursor = cursor.endsWith("Z") ? cursor.substring(0, cursor.length() - 1) : cursor;
            return LocalDateTime.parse(cleanCursor, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            log.warn("Invalid cursor format: {}", cursor);
            return LocalDateTime.now();
        }
    }
}
