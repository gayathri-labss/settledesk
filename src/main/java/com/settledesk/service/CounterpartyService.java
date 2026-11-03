package com.settledesk.service;

import com.settledesk.dto.CounterpartyResponse;
import com.settledesk.model.Counterparty;
import com.settledesk.repository.CounterpartyRepository;
import com.settledesk.specification.CounterpartySpecification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CounterpartyService {

    private final CounterpartyRepository counterpartyRepository;

    public CounterpartyService(
            CounterpartyRepository counterpartyRepository) {

        this.counterpartyRepository = counterpartyRepository;
    }

    @Transactional(readOnly = true)
    public Page<CounterpartyResponse> searchCounterparties(
            String code,
            String name,
            String type,
            String country,
            Boolean active,
            Pageable pageable) {

        Specification<Counterparty> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.conjunction();

        if (code != null && !code.isBlank()) {
            specification = specification.and(
                    CounterpartySpecification.hasCounterpartyCode(code)
            );
        }

        if (name != null && !name.isBlank()) {
            specification = specification.and(
                    CounterpartySpecification.hasCounterpartyName(name)
            );
        }

        if (type != null && !type.isBlank()) {
            specification = specification.and(
                    CounterpartySpecification.hasCounterpartyType(type)
            );
        }

        if (country != null && !country.isBlank()) {
            specification = specification.and(
                    CounterpartySpecification.hasCountry(country)
            );
        }

        if (active != null) {
            specification = specification.and(
                    CounterpartySpecification.isActive(active)
            );
        }

        Page<Counterparty> counterparties =
                counterpartyRepository.findAll(
                        specification, pageable
                );

        return counterparties.map(this::toResponse);
    }

    private CounterpartyResponse toResponse(
            Counterparty counterparty) {

        return CounterpartyResponse.builder()
                .counterpartyId(counterparty.getCounterpartyId())
                .counterpartyCode(counterparty.getCounterpartyCode())
                .counterpartyName(counterparty.getCounterpartyName())
                .counterpartyType(counterparty.getCounterpartyType())
                .country(counterparty.getCountry())
                .active(counterparty.isActive())
                .build();
    }
}