package com.dolpin.domain.place.service;

import com.dolpin.domain.place.client.PlaceAiClient;
import com.dolpin.domain.place.dto.response.PlaceAiResponse;
import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.entity.Place;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceQueryServiceImpl implements PlaceQueryService {

    private final PlaceRepository placeRepository;
    private final PlaceAiClient placeAiClient;

    // 기본 검색 반경: 700m
    private static final double DEFAULT_SEARCH_RADIUS = 700.0;

    @Override
    @Transactional(readOnly = true)
    public PlaceSearchResponse searchPlaces(String query, Double lat, Double lng, String category) {
        // 검색어 유효성 검사
        if (query == null || query.trim().isEmpty()) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("검색어를 입력해주세요"));
        }

        // 위치 유효성 검사
        if (lat == null || lng == null) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("위치 정보가 필요합니다"));
        }

        // AI 서비스에 검색 쿼리 전송
        PlaceAiResponse aiResponse = placeAiClient.searchPlacesByQuery(query);

        if (aiResponse == null || aiResponse.getData() == null || aiResponse.getData().isEmpty()) {
            return PlaceSearchResponse.builder()
                    .total(0)
                    .places(Collections.emptyList())
                    .build();
        }

        // 추천된 장소 ID 목록 추출
        List<Long> placeIds = aiResponse.getData().stream()
                .map(PlaceAiResponse.PlaceRecommendation::getPlaceId)
                .collect(Collectors.toList());

        // 유사도 점수 매핑 (장소 ID -> 유사도 점수)
        Map<Long, Double> similarityScores = aiResponse.getData().stream()
                .collect(Collectors.toMap(
                        PlaceAiResponse.PlaceRecommendation::getPlaceId,
                        PlaceAiResponse.PlaceRecommendation::getSimilarityScore
                ));

        // PostGIS를 사용하여 반경 내 장소 조회 및 거리 계산
        List<Object[]> nearbyPlaces = placeRepository.findPlacesWithinRadiusByIds(
                placeIds, lat, lng, DEFAULT_SEARCH_RADIUS);

        // DTO 변환
        List<PlaceSearchResponse.PlaceDto> placeDtos = new ArrayList<>();
        for (Object[] result : nearbyPlaces) {
            Place place = (Place) result[0];
            Double distance = (Double) result[1];

            // 거리 포맷팅
            String formattedDistance = formatDistance(distance);

            // 키워드 추출
            List<String> keywordList = place.getKeywords().stream()
                    .map(pk -> pk.getKeyword().getKeyword())
                    .collect(Collectors.toList());

            // 위치 정보 변환
            Point location = place.getLocation();
            Map<String, Object> locationMap = new HashMap<>();
            locationMap.put("type", "Point");
            locationMap.put("coordinates", new double[]{location.getX(), location.getY()});

            // 유사도 점수 가져오기
            Double similarityScore = similarityScores.getOrDefault(place.getId(), 0.0);

            // DTO 생성
            PlaceSearchResponse.PlaceDto placeDto = PlaceSearchResponse.PlaceDto.builder()
                    .id(place.getId())
                    .name(place.getName())
                    .thumbnail(place.getImageUrl())
                    .distance(formattedDistance)
                    .momentCount("0")  // 추후 연동 필요
                    .keywords(keywordList)
                    .location(locationMap)
                    .similarityScore(similarityScore)
                    .build();

            placeDtos.add(placeDto);
        }

        // 유사도 점수 기준 정렬
        placeDtos.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));

        return PlaceSearchResponse.builder()
                .total(placeDtos.size())
                .places(placeDtos)
                .build();
    }

    /**
     * 거리 포맷팅 (m 또는 km 단위)
     */
    private String formatDistance(Double distanceInMeters) {
        if (distanceInMeters == null) return "0";

        if (distanceInMeters < 1000) {
            // 미터 단위로 표시
            return Math.round(distanceInMeters) + "m";
        } else {
            // 킬로미터 단위로 표시
            BigDecimal km = BigDecimal.valueOf(distanceInMeters / 1000.0)
                    .setScale(1, RoundingMode.HALF_UP);
            return km.toString() + "km";
        }
    }
}