package com.settledesk.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentResponse {

    private Long instrumentId;

    private String instrumentCode;

    private String instrumentName;

    private String instrumentType;

    private String currency;

    private String isin;

    private boolean active;
}