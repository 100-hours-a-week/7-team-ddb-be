package com.dolpin.domain.moment.service.command;

import com.dolpin.domain.moment.dto.request.MomentCreateRequest;
import com.dolpin.domain.moment.dto.request.MomentUpdateRequest;
import com.dolpin.domain.moment.dto.response.MomentCreateResponse;
import com.dolpin.domain.moment.dto.response.MomentUpdateResponse;

public interface MomentCommandService {

    MomentCreateResponse createMoment(Long userId, MomentCreateRequest request);

    MomentUpdateResponse updateMoment(Long userId, Long momentId, MomentUpdateRequest request);

    void deleteMoment(Long userId, Long momentId);
}
