package com.dolpin.domain.place.service.template;

import com.dolpin.domain.place.repository.PlaceRepository;
import org.springframework.stereotype.Component;

@Component
public class SimpleBusinessStatusQuery extends BusinessStatusQueryTemplate {

    public SimpleBusinessStatusQuery(PlaceRepository placeRepository) {
        super(placeRepository);
    }

    @Override
    protected BusinessStatusContext collectBusinessStatusInformation(Long placeId) {
        // 영업 상태 판단에 필요한 최소한의 정보만 조회
        return BusinessStatusContext.builder()
                .placeId(placeId)
                .hours(getHours(placeId)) // 운영시간만 조회
                .build();
    }
}
