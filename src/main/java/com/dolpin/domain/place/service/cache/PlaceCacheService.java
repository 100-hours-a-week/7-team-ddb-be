package com.dolpin.domain.place.service.cache;

import com.dolpin.domain.place.dto.response.PlaceCategoryResponse;
import com.dolpin.domain.place.entity.Place;
import com.dolpin.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceCacheService {

    private final PlaceRepository placeRepository;

    @Cacheable(value = "place-categories", key = "'all'")
    @Transactional(readOnly = true)
    public PlaceCategoryResponse getAllCategoriesWithCache() {
        log.info("Fetching place categories from database (cache miss)");

        List<String> categories = placeRepository.findDistinctCategories();

        PlaceCategoryResponse response = PlaceCategoryResponse.builder()
                .categories(categories)
                .build();

        log.info("Cached {} place categories", categories.size());
        return response;
    }

    @Cacheable(value = "places-by-category", key = "#category")
    @Transactional(readOnly = true)
    public List<Place> getPlacesByCategoryWithCache(String category) {
        log.info("Fetching places by category '{}' from database (cache miss)", category);

        // 카테고리별 모든 장소 조회 (키워드 포함)
        List<Place> places = placeRepository.findByCategoryWithKeywords(category);

        log.info("Cached {} places for category '{}'", places.size(), category);
        return places;
    }

    @Cacheable(value = "places-basic", key = "T(java.util.Objects).hash(#placeIds)")
    @Transactional(readOnly = true)
    public List<Place> getPlacesByIdsWithCache(List<Long> placeIds) {
        if (placeIds.isEmpty()) {
            return List.of();
        }

        log.info("Fetching {} places by IDs from database (cache miss)", placeIds.size());

        List<Place> places = placeRepository.findByIdsWithKeywords(placeIds);

        log.info("Cached {} places by IDs", places.size());
        return places;
    }


    @CacheEvict(value = "place-categories", key = "'all'")
    public void evictCategoriesCache() {
        log.info("🗑️ Evicted place categories cache");
    }

    @CacheEvict(value = "places-by-category", key = "#category")
    public void evictPlacesByCategoryCache(String category) {
        log.info("🗑️ Evicted places cache for category: {}", category);
    }

    @CacheEvict(value = {"place-categories", "places-by-category", "places-basic"}, allEntries = true)
    public void evictAllPlaceCache() {
        log.info("🗑️ Evicted all place related caches");
    }
}
