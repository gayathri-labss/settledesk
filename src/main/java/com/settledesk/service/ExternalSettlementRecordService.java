package com.settledesk.service;

import com.settledesk.dto.CreateExternalSettlementRequest;
import com.settledesk.model.ExternalSettlementRecord;
import com.settledesk.repository.ExternalSettlementRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExternalSettlementRecordService {

    private final ExternalSettlementRecordRepository repository;

    public ExternalSettlementRecord createExternalRecord(
            CreateExternalSettlementRequest request) {

        if (repository.existsBySourceAndExternalReference(
                request.getSource(),
                request.getExternalReference())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "External record already exists"
            );
        }

        ExternalSettlementRecord record = ExternalSettlementRecord.builder()
                .externalReference(request.getExternalReference())
                .settlementReference(request.getSettlementReference())
                .source(request.getSource())
                .instrument(request.getInstrument())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .settlementDate(request.getSettlementDate())
                .status(request.getStatus())
                .receivedAt(LocalDateTime.now())
                .build();

        return repository.save(record);
    }

    public List<ExternalSettlementRecord> getAllExternalRecords() {
        return repository.findAll();
    }

    public ExternalSettlementRecord getExternalRecordById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "External settlement record not found"
                ));
    }
}