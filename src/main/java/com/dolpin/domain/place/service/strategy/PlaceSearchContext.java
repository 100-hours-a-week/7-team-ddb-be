package com.dolpin.domain.place.service.strategy;

import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import com.dolpin.global.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PlaceSearchContext {
    private final String query;
    private final Double lat;
    private final Double lng;
    private final String category;
    private final Long userId;
    private final String devToken;

    public boolean hasQuery() {
        return StringUtils.isNotBlank(query);
    }

    public boolean hasCategory() {
        return StringUtils.isNotBlank(category);
    }

    public PlaceSearchType determineSearchType() {
        if (hasQuery()) {
            return PlaceSearchType.AI_QUERY;
        } else if (hasCategory()) {
            return PlaceSearchType.CATEGORY;
        } else {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER,
                    "검색어 또는 카테고리가 필요합니다");
        }
    }

    public void validate() {
        boolean hasQuery = hasQuery();
        boolean hasCategory = hasCategory();

        if (hasQuery && hasCategory) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER,
                    "검색어와 카테고리 중 하나만 선택해주세요");
        }

        if (!hasQuery && !hasCategory) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER,
                    "검색어 또는 카테고리가 필요합니다");
        }

        if (lat == null || lng == null) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER,
                    "위치 정보가 필요합니다");
        }
    }
}

