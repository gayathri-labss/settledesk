package com.settledesk.service;

import com.settledesk.dto.OperationResponse;
import com.settledesk.model.ProcessingJob;
import com.settledesk.repository.ProcessingJobRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OperationService {

    private final ProcessingJobRepository processingJobRepository;

    public OperationResponse getOperationById(Long operationId) {

        ProcessingJob job = processingJobRepository.findById(operationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Operation not found with ID: " + operationId
                ));

        return OperationResponse.builder()
                .operationId(job.getJobId())
                .operationType(job.getJobType())
                .status(job.getStatus())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }
}