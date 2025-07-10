package com.dolpin.domain.place.service.strategy;

import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

@Slf4j
@Component
public class PlaceSearchStrategyFactory {

    private final List<PlaceSearchStrategy> strategies;

    public PlaceSearchStrategyFactory(List<PlaceSearchStrategy> strategies) {
        this.strategies = strategies.stream()
                .sorted(Comparator.comparingInt(PlaceSearchStrategy::getPriority))
                .collect(Collectors.toList());

        log.info("검색 전략 등록 완료: {}",
                strategies.stream()
                        .map(s -> s.getClass().getSimpleName())
                        .collect(Collectors.joining(", ")));
    }

    public PlaceSearchStrategy getStrategy(PlaceSearchType searchType) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(searchType))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResponseStatus.INVALID_PARAMETER,
                        "지원하지 않는 검색 타입: " + searchType));
    }
}

