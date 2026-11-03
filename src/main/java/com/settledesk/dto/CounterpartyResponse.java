package com.settledesk.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounterpartyResponse {

    private Long counterpartyId;

    private String counterpartyCode;

    private String counterpartyName;

    private String counterpartyType;

    private String country;

    private boolean active;
}