package com.settledesk.controller;

import com.settledesk.dto.CounterpartyResponse;
import com.settledesk.service.CounterpartyService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/counterparties")
public class CounterpartyController {

    private final CounterpartyService counterpartyService;

    public CounterpartyController(
            CounterpartyService counterpartyService) {

        this.counterpartyService = counterpartyService;
    }

    @GetMapping
    public ResponseEntity<Page<CounterpartyResponse>> searchCounterparties(

            @RequestParam(required = false) String code,

            @RequestParam(required = false) String name,

            @RequestParam(required = false) String type,

            @RequestParam(required = false) String country,

            @RequestParam(required = false) Boolean active,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "10") int size,

            @RequestParam(defaultValue = "counterpartyId") String sortBy,

            @RequestParam(defaultValue = "asc") String direction) {

        Set<String> allowedSortFields = Set.of(
                "counterpartyId",
                "counterpartyCode",
                "counterpartyName",
                "counterpartyType",
                "country"
        );

        if (!allowedSortFields.contains(sortBy)) {
            return ResponseEntity.badRequest().build();
        }

        if (!direction.equalsIgnoreCase("asc")
                && !direction.equalsIgnoreCase("desc")) {
            return ResponseEntity.badRequest().build();
        }

        if (page < 0 || size < 1 || size > 100) {
            return ResponseEntity.badRequest().build();
        }

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(
                page, size, sort
        );

        Page<CounterpartyResponse> counterparties =
                counterpartyService.searchCounterparties(
                        code,
                        name,
                        type,
                        country,
                        active,
                        pageable
                );

        return ResponseEntity.ok(counterparties);
    }
}