package com.dolpin.domain.moment.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMomentGenerationRequest {
    private Long id;
    private String name;
    private String address;
    private String thumbnail;
    private List<String> keyword;
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
        private String breakTime;
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
