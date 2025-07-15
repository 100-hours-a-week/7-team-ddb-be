package com.dolpin.domain.place.service.template;

import com.dolpin.domain.place.entity.PlaceHours;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class BusinessStatusContext {
    private final Long placeId;
    private final List<PlaceHours> hours;
}
