package com.dolpin.domain.place.service.query;

import com.dolpin.domain.place.dto.response.PlaceAiResponse;
import com.dolpin.domain.place.dto.response.PlaceCategoryResponse;
import com.dolpin.domain.place.dto.response.PlaceDetailResponse;
import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.entity.Place;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import org.locationtech.jts.geom.Point;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

public interface PlaceQueryService {

    PlaceSearchResponse searchPlaces(String query, Double lat, Double lng, String category);

    PlaceDetailResponse getPlaceDetail(Long placeId);

    PlaceCategoryResponse getAllCategories();
}

