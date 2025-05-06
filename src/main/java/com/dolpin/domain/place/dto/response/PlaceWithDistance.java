package com.dolpin.domain.place.dto.response;

import com.dolpin.domain.place.entity.Place;

public interface PlaceWithDistance {
    Place getPlace();
    Double getDistance();
}