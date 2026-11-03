package com.settledesk.specification;

import com.settledesk.model.Instrument;
import org.springframework.data.jpa.domain.Specification;

public class InstrumentSpecification {

    public static Specification<Instrument> hasInstrumentCode(
            String code) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(
                                root.get("instrumentCode")),
                        "%" + code.toLowerCase() + "%"
                );
    }

    public static Specification<Instrument> hasInstrumentName(
            String name) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(
                                root.get("instrumentName")),
                        "%" + name.toLowerCase() + "%"
                );
    }

    public static Specification<Instrument> hasInstrumentType(
            String type) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        criteriaBuilder.lower(
                                root.get("instrumentType")),
                        type.toLowerCase()
                );
    }

    public static Specification<Instrument> hasCurrency(
            String currency) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        criteriaBuilder.lower(
                                root.get("currency")),
                        currency.toLowerCase()
                );
    }

    public static Specification<Instrument> isActive(
            Boolean active) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("active"),
                        active
                );
    }
}