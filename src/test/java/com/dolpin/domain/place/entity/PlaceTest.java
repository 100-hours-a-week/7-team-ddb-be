package com.dolpin.domain.place.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Place 엔티티 테스트")
class PlaceTest {

    @Test
    @DisplayName("Place 생성 시 createdAt, updatedAt이 자동 설정된다")
    void prePersist_SetsTimestamps() {

        Place place = new Place();

        place.prePersist();

        assertThat(place.getCreatedAt()).isNotNull();
        assertThat(place.getUpdatedAt()).isNotNull();
        assertThat(place.getCreatedAt()).isEqualTo(place.getUpdatedAt());
    }

    @Test
    @DisplayName("Place 수정 시 updatedAt이 갱신된다")
    void preUpdate_UpdatesTimestamp() throws InterruptedException {

        Place place = new Place();
        place.prePersist();
        LocalDateTime originalUpdatedAt = place.getUpdatedAt();

        Thread.sleep(1);

        place.preUpdate();

        assertThat(place.getUpdatedAt()).isAfter(originalUpdatedAt);
    }
}
