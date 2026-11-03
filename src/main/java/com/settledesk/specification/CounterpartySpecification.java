package com.settledesk.specification;

import com.settledesk.model.Counterparty;
import org.springframework.data.jpa.domain.Specification;

public class CounterpartySpecification {

    public static Specification<Counterparty> hasCounterpartyCode(
            String code) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("counterpartyCode")),
                        "%" + code.toLowerCase() + "%"
                );
    }

    public static Specification<Counterparty> hasCounterpartyName(
            String name) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("counterpartyName")),
                        "%" + name.toLowerCase() + "%"
                );
    }

    public static Specification<Counterparty> hasCounterpartyType(
            String type) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("counterpartyType")),
                        type.toLowerCase()
                );
    }

    public static Specification<Counterparty> hasCountry(
            String country) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("country")),
                        country.toLowerCase()
                );
    }

    public static Specification<Counterparty> isActive(
            Boolean active) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("active"), active);
    }
}