package com.settledesk.specification;
import com.settledesk.model.Trade;
import org.springframework.data.jpa.domain.Specification;

public class TradeSpecification {

    public static Specification<Trade> hasInstrument(String instrument) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("instrument"), instrument);
    }

    public static Specification<Trade> hasBuyer(String buyer) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("buyer"), buyer);
    }
}
