package com.dolpin.domain.place.dto.response;

public interface PlaceWithDistance {
    Long getId();
    String getName();
    String getCategory();
    String getRoadAddress();
    String getLotAddress();
    Double getDistance();
    // 위치 좌표 정보 추가
    Double getLongitude();  // X 좌표 (경도)
    Double getLatitude();   // Y 좌표 (위도)
}