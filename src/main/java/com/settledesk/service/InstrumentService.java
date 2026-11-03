package com.settledesk.service;

import com.settledesk.dto.InstrumentResponse;
import com.settledesk.model.Instrument;
import com.settledesk.repository.InstrumentRepository;
import com.settledesk.specification.InstrumentSpecification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;

    public InstrumentService(
            InstrumentRepository instrumentRepository) {

        this.instrumentRepository = instrumentRepository;
    }

    @Transactional(readOnly = true)
    public Page<InstrumentResponse> searchInstruments(
            String code,
            String name,
            String type,
            String currency,
            Boolean active,
            Pageable pageable) {

        Specification<Instrument> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.conjunction();

        if (code != null && !code.isBlank()) {
            specification = specification.and(
                    InstrumentSpecification.hasInstrumentCode(code)
            );
        }

        if (name != null && !name.isBlank()) {
            specification = specification.and(
                    InstrumentSpecification.hasInstrumentName(name)
            );
        }

        if (type != null && !type.isBlank()) {
            specification = specification.and(
                    InstrumentSpecification.hasInstrumentType(type)
            );
        }

        if (currency != null && !currency.isBlank()) {
            specification = specification.and(
                    InstrumentSpecification.hasCurrency(currency)
            );
        }

        if (active != null) {
            specification = specification.and(
                    InstrumentSpecification.isActive(active)
            );
        }

        Page<Instrument> instruments =
                instrumentRepository.findAll(
                        specification, pageable
                );

        return instruments.map(this::toResponse);
    }

    private InstrumentResponse toResponse(Instrument instrument) {

        return InstrumentResponse.builder()
                .instrumentId(instrument.getInstrumentId())
                .instrumentCode(instrument.getInstrumentCode())
                .instrumentName(instrument.getInstrumentName())
                .instrumentType(instrument.getInstrumentType())
                .currency(instrument.getCurrency())
                .isin(instrument.getIsin())
                .active(instrument.isActive())
                .build();
    }
}