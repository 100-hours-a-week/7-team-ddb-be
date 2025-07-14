package com.dolpin.domain.moment.service.command;

import com.dolpin.domain.moment.dto.request.MomentCreateRequest;
import com.dolpin.domain.moment.dto.request.MomentUpdateRequest;
import com.dolpin.domain.moment.dto.response.MomentCreateResponse;
import com.dolpin.domain.moment.dto.response.MomentUpdateResponse;
import com.dolpin.domain.moment.service.template.MomentCreateOperation;
import com.dolpin.domain.moment.service.template.MomentDeleteOperation;
import com.dolpin.domain.moment.service.template.MomentOperationContext;
import com.dolpin.domain.moment.service.template.MomentUpdateOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomentCommandServiceImpl implements MomentCommandService {

    private final MomentCreateOperation momentCreateOperation;
    private final MomentUpdateOperation momentUpdateOperation;
    private final MomentDeleteOperation momentDeleteOperation;

    @Override
    public MomentCreateResponse createMoment(Long userId, MomentCreateRequest request) {
        MomentOperationContext context = MomentOperationContext.fromCreateRequest(userId, request);
        return momentCreateOperation.executeMomentOperation(context);
    }

    @Override
    public MomentUpdateResponse updateMoment(Long userId, Long momentId, MomentUpdateRequest request) {
        MomentOperationContext context = MomentOperationContext.fromUpdateRequest(userId, momentId, request);
        return momentUpdateOperation.executeMomentOperation(context);
    }

    @Override
    public void deleteMoment(Long userId, Long momentId) {
        MomentOperationContext context = MomentOperationContext.forDelete(userId, momentId);
        momentDeleteOperation.executeMomentOperation(context);
    }
}
