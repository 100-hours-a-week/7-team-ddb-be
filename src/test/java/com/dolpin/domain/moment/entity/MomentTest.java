package com.dolpin.domain.moment.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.dolpin.global.constants.MomentTestConstants.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Moment Entity 테스트")
class MomentTest {

    private Moment moment;

    @BeforeEach
    void setUp() {
        moment = Moment.builder()
                .id(TEST_MOMENT_ID)
                .userId(TEST_USER_ID)
                .title(TEST_MOMENT_TITLE)
                .content(TEST_MOMENT_CONTENT)
                .placeId(TEST_PLACE_ID)
                .placeName(TEST_PLACE_NAME)
                .isPublic(DEFAULT_IS_PUBLIC)
                .viewCount(DEFAULT_VIEW_COUNT)
                .build();
    }

    @Test
    @DisplayName("Moment 생성 시 기본값 설정")
    void createMoment_DefaultValues() {
        // given & when
        Moment newMoment = Moment.builder()
                .userId(TEST_USER_ID)
                .title(TEST_MOMENT_TITLE)
                .content(TEST_MOMENT_CONTENT)
                .build();

        // then
        assertThat(newMoment.getIsPublic()).isTrue();
        assertThat(newMoment.getViewCount()).isEqualTo(0L);
        assertThat(newMoment.getImages()).isEmpty();
    }

    @Test
    @DisplayName("내용 업데이트")
    void updateContent_Success() {
        // when
        moment.updateContent(UPDATED_MOMENT_TITLE, UPDATED_MOMENT_CONTENT, UPDATED_IS_PUBLIC);

        // then
        assertThat(moment.getTitle()).isEqualTo(UPDATED_MOMENT_TITLE);
        assertThat(moment.getContent()).isEqualTo(UPDATED_MOMENT_CONTENT);
        assertThat(moment.getIsPublic()).isEqualTo(UPDATED_IS_PUBLIC);
    }

    @Test
    @DisplayName("내용 업데이트 - null 값 무시")
    void updateContent_NullValues() {
        // given
        String originalTitle = moment.getTitle();
        String originalContent = moment.getContent();
        Boolean originalIsPublic = moment.getIsPublic();

        // when
        moment.updateContent(null, null, null);

        // then
        assertThat(moment.getTitle()).isEqualTo(originalTitle);
        assertThat(moment.getContent()).isEqualTo(originalContent);
        assertThat(moment.getIsPublic()).isEqualTo(originalIsPublic);
    }

    @Test
    @DisplayName("내용 업데이트 - 빈 문자열 무시")
    void updateContent_EmptyStrings() {
        // given
        String originalTitle = moment.getTitle();
        String originalContent = moment.getContent();

        // when
        moment.updateContent("", "   ", DEFAULT_IS_PUBLIC);

        // then
        assertThat(moment.getTitle()).isEqualTo(originalTitle);
        assertThat(moment.getContent()).isEqualTo(originalContent);
        assertThat(moment.getIsPublic()).isEqualTo(DEFAULT_IS_PUBLIC);
    }

    @Test
    @DisplayName("장소 정보 업데이트")
    void updatePlaceInfo_Success() {
        // when
        moment.updatePlaceInfo(UPDATED_PLACE_ID, UPDATED_PLACE_NAME);

        // then
        assertThat(moment.getPlaceId()).isEqualTo(UPDATED_PLACE_ID);
        assertThat(moment.getPlaceName()).isEqualTo(UPDATED_PLACE_NAME);
    }

    @Test
    @DisplayName("장소 정보 업데이트 - null 값 무시")
    void updatePlaceInfo_NullValues() {
        // given
        Long originalPlaceId = moment.getPlaceId();
        String originalPlaceName = moment.getPlaceName();

        // when
        moment.updatePlaceInfo(null, null);

        // then
        assertThat(moment.getPlaceId()).isEqualTo(originalPlaceId);
        assertThat(moment.getPlaceName()).isEqualTo(originalPlaceName);
    }

    @Test
    @DisplayName("조회수 증가")
    void incrementViewCount_Success() {
        // given
        Long initialViewCount = moment.getViewCount();

        // when
        moment.incrementViewCount();

        // then
        assertThat(moment.getViewCount()).isEqualTo(initialViewCount + 1);
    }

    @Test
    @DisplayName("조회수 증가 - 0에서 시작")
    void incrementViewCount_FromZero() {
        // given
        Moment momentWithZeroViewCount = Moment.builder()
                .userId(TEST_USER_ID)
                .title(TEST_MOMENT_TITLE)
                .content(TEST_MOMENT_CONTENT)
                .viewCount(0L)
                .build();

        // when
        momentWithZeroViewCount.incrementViewCount();

        // then
        assertThat(momentWithZeroViewCount.getViewCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("이미지 추가")
    void addImage_Success() {
        // when
        moment.addImage(TEST_IMAGE_1);

        // then
        assertThat(moment.getImages()).hasSize(1);
        assertThat(moment.getImages().get(0).getImageUrl()).isEqualTo(TEST_IMAGE_1);
        assertThat(moment.getImages().get(0).getImageSequence()).isEqualTo(0);
    }

    @Test
    @DisplayName("여러 이미지 추가")
    void addImages_Success() {
        // when
        moment.addImages(TEST_IMAGES);

        // then
        assertThat(moment.getImages()).hasSize(TEST_IMAGES_COUNT);
        assertThat(moment.getImages().get(0).getImageUrl()).isEqualTo(TEST_IMAGE_1);
        assertThat(moment.getImages().get(1).getImageUrl()).isEqualTo(TEST_IMAGE_2);
        assertThat(moment.getImages().get(0).getImageSequence()).isEqualTo(0);
        assertThat(moment.getImages().get(1).getImageSequence()).isEqualTo(1);
    }

    @Test
    @DisplayName("null 이미지 리스트 추가")
    void addImages_NullList() {
        // when
        moment.addImages(null);

        // then
        assertThat(moment.getImages()).isEmpty();
    }

    @Test
    @DisplayName("빈 이미지 리스트 추가")
    void addImages_EmptyList() {
        // when
        moment.addImages(java.util.Arrays.asList());

        // then
        assertThat(moment.getImages()).isEmpty();
    }

    @Test
    @DisplayName("이미지 모두 삭제")
    void clearImages_Success() {
        // given
        moment.addImages(TEST_IMAGES);
        assertThat(moment.getImages()).hasSize(TEST_IMAGES_COUNT);

        // when
        moment.clearImages();

        // then
        assertThat(moment.getImages()).isEmpty();
    }

    @Test
    @DisplayName("이미지 교체")
    void replaceImages_Success() {
        // given
        moment.addImages(TEST_IMAGES);
        assertThat(moment.getImages()).hasSize(TEST_IMAGES_COUNT);

        // when
        moment.replaceImages(UPDATED_IMAGES);

        // then
        assertThat(moment.getImages()).hasSize(UPDATED_IMAGES_COUNT);
        assertThat(moment.getImages().get(0).getImageUrl()).isEqualTo(UPDATED_IMAGE);
    }

    @Test
    @DisplayName("이미지 교체 - null로 교체")
    void replaceImages_WithNull() {
        // given
        moment.addImages(TEST_IMAGES);
        assertThat(moment.getImages()).hasSize(TEST_IMAGES_COUNT);

        // when
        moment.replaceImages(null);

        // then
        assertThat(moment.getImages()).isEmpty();
    }

    @Test
    @DisplayName("공개/비공개 토글")
    void togglePublic_Success() {
        // given
        Boolean originalPublic = moment.getIsPublic();

        // when
        moment.togglePublic();

        // then
        assertThat(moment.getIsPublic()).isEqualTo(!originalPublic);
    }

    @Test
    @DisplayName("공개 상태 설정")
    void setPublic_Success() {
        // when
        moment.setPublic(false);

        // then
        assertThat(moment.getIsPublic()).isFalse();

        // when
        moment.setPublic(true);

        // then
        assertThat(moment.getIsPublic()).isTrue();
    }

    @Test
    @DisplayName("이미지 존재 여부 확인")
    void hasImages_Success() {
        // given
        assertThat(moment.hasImages()).isFalse();

        // when
        moment.addImage(TEST_IMAGE_1);

        // then
        assertThat(moment.hasImages()).isTrue();
    }

    @Test
    @DisplayName("썸네일 URL 가져오기")
    void getThumbnailUrl_Success() {
        // given
        assertThat(moment.getThumbnailUrl()).isNull();

        // when
        moment.addImages(TEST_IMAGES);

        // then
        assertThat(moment.getThumbnailUrl()).isEqualTo(TEST_IMAGE_1);
    }

    @Test
    @DisplayName("이미지 개수 가져오기")
    void getImageCount_Success() {
        // given
        assertThat(moment.getImageCount()).isEqualTo(0);

        // when
        moment.addImages(TEST_IMAGES);

        // then
        assertThat(moment.getImageCount()).isEqualTo(TEST_IMAGES_COUNT);
    }

    @Test
    @DisplayName("소유자 확인")
    void isOwnedBy_Success() {
        // when & then
        assertThat(moment.isOwnedBy(TEST_USER_ID)).isTrue();
        assertThat(moment.isOwnedBy(OTHER_USER_ID)).isFalse();
    }

    @Test
    @DisplayName("조회 가능 여부 확인 - 공개 기록")
    void canBeViewedBy_PublicMoment() {
        // given
        moment.setPublic(true);

        // when & then
        assertThat(moment.canBeViewedBy(TEST_USER_ID)).isTrue();
        assertThat(moment.canBeViewedBy(OTHER_USER_ID)).isTrue();
        assertThat(moment.canBeViewedBy(null)).isTrue();
    }

    @Test
    @DisplayName("조회 가능 여부 확인 - 비공개 기록")
    void canBeViewedBy_PrivateMoment() {
        // given
        moment.setPublic(false);

        // when & then
        assertThat(moment.canBeViewedBy(TEST_USER_ID)).isTrue();
        assertThat(moment.canBeViewedBy(OTHER_USER_ID)).isFalse();
        assertThat(moment.canBeViewedBy(null)).isFalse();
    }

    @Test
    @DisplayName("조회수 getter - 기본값 확인")
    void getViewCount_DefaultValue() {
        // given
        Moment momentWithDefaultViewCount = Moment.builder()
                .userId(TEST_USER_ID)
                .title(TEST_MOMENT_TITLE)
                .content(TEST_MOMENT_CONTENT)
                .build();

        // when & then
        assertThat(momentWithDefaultViewCount.getViewCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("PrePersist 동작 확인")
    void prePersist_SetsDefaults() {
        // given
        Moment newMoment = Moment.builder()
                .userId(TEST_USER_ID)
                .title(TEST_MOMENT_TITLE)
                .content(TEST_MOMENT_CONTENT)
                .build();

        // when
        newMoment.prePersist();

        // then
        assertThat(newMoment.getCreatedAt()).isNotNull();
        assertThat(newMoment.getUpdatedAt()).isNotNull();
        assertThat(newMoment.getViewCount()).isEqualTo(0L);
        assertThat(newMoment.getCreatedAt()).isEqualTo(newMoment.getUpdatedAt());
    }

    @Test
    @DisplayName("PreUpdate 동작 확인")
    void preUpdate_UpdatesTimestamp() {
        // given
        moment.prePersist();
        java.time.LocalDateTime originalCreatedAt = moment.getCreatedAt();
        java.time.LocalDateTime originalUpdatedAt = moment.getUpdatedAt();

        // 시간 경과 시뮬레이션을 위한 잠시 대기
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // when
        moment.preUpdate();

        // then
        assertThat(moment.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(moment.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(moment.getViewCount()).isEqualTo(DEFAULT_VIEW_COUNT);
    }

    @Test
    @DisplayName("이미지 순서 유지 확인")
    void imageSequence_MaintainsOrder() {
        // when
        moment.addImages(ORDERED_IMAGES);

        // then
        assertThat(moment.getImages()).hasSize(3);
        assertThat(moment.getImages().get(0).getImageSequence()).isEqualTo(0);
        assertThat(moment.getImages().get(1).getImageSequence()).isEqualTo(1);
        assertThat(moment.getImages().get(2).getImageSequence()).isEqualTo(2);

        assertThat(moment.getImages().get(0).getImageUrl()).isEqualTo(FIRST_IMAGE);
        assertThat(moment.getImages().get(1).getImageUrl()).isEqualTo(SECOND_IMAGE);
        assertThat(moment.getImages().get(2).getImageUrl()).isEqualTo(THIRD_IMAGE);
    }

    @Test
    @DisplayName("Builder 기본값 확인")
    void builder_DefaultValues() {
        // given & when
        Moment builtMoment = Moment.builder()
                .userId(TEST_USER_ID)
                .title(TEST_MOMENT_TITLE)
                .content(TEST_MOMENT_CONTENT)
                .build();

        // then
        assertThat(builtMoment.getIsPublic()).isTrue();
        assertThat(builtMoment.getViewCount()).isEqualTo(0L);
        assertThat(builtMoment.getImages()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("PrePersist에서 null viewCount 처리 확인")
    void prePersist_HandlesNullViewCount() {

        Moment newMoment = Moment.builder()
                .userId(TEST_USER_ID)
                .title(TEST_MOMENT_TITLE)
                .content(TEST_MOMENT_CONTENT)
                .viewCount(0L)
                .build();

        // when
        newMoment.prePersist();

        // then
        assertThat(newMoment.getViewCount()).isEqualTo(0L);
        assertThat(newMoment.getCreatedAt()).isNotNull();
        assertThat(newMoment.getUpdatedAt()).isNotNull();
    }
}
