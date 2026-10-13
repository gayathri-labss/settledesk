package com.settledesk.model;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Trade {
    private UUID tradeId;
    private String instrument;
    private BigDecimal quantity;
    private BigDecimal price;
    private String buyer;
    private String seller;}
