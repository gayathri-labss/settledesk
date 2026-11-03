package com.settledesk.controller;

import com.settledesk.dto.InstrumentResponse;
import com.settledesk.service.InstrumentService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;

    public InstrumentController(
            InstrumentService instrumentService) {

        this.instrumentService = instrumentService;
    }

    @GetMapping
    public ResponseEntity<Page<InstrumentResponse>> searchInstruments(

            @RequestParam(required = false) String code,

            @RequestParam(required = false) String name,

            @RequestParam(required = false) String type,

            @RequestParam(required = false) String currency,

            @RequestParam(required = false) Boolean active,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "10") int size,

            @RequestParam(defaultValue = "instrumentId") String sortBy,

            @RequestParam(defaultValue = "asc") String direction) {

        Set<String> allowedSortFields = Set.of(
                "instrumentId",
                "instrumentCode",
                "instrumentName",
                "instrumentType",
                "currency"
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

        Page<InstrumentResponse> instruments =
                instrumentService.searchInstruments(
                        code,
                        name,
                        type,
                        currency,
                        active,
                        pageable
                );

        return ResponseEntity.ok(instruments);
    }

    @GetMapping("/{instrumentId}")
    public ResponseEntity<InstrumentResponse> getInstrumentById(
            @PathVariable Long instrumentId) {

        InstrumentResponse instrument =
                instrumentService.getInstrumentById(instrumentId);

        return ResponseEntity.ok(instrument);
    }
}