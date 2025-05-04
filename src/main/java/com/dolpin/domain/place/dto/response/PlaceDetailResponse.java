package com.dolpin.domain.place.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceDetailResponse {

    private Long id;
    private String name;
    private String address;
    private Map<String, Object> location;
    private List<String> keywords;
    private String description;

    @JsonProperty("opening_hours")
    private OpeningHours openingHours;

    private String phone;
    private List<Menu> menu;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpeningHours {
        private String status;
        private List<Schedule> schedules;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Schedule {
        private String day;
        private String hours;
        private String note;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Menu {
        private String name;
        private Integer price;
    }
}