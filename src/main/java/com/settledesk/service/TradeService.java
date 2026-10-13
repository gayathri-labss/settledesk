package com.settledesk.service;
import com.settledesk.dto.CreateTradeRequest;
import com.settledesk.model.Trade;
import org.springframework.stereotype.Service;
import com.settledesk.repository.TradeRepository;

@Service
public class TradeService {

    private final TradeRepository tradeRepository;

    public TradeService(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    public Trade createTrade(CreateTradeRequest request) {


        return tradeRepository.save(Trade.builder()
                .instrument(request.getInstrument())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .buyer(request.getBuyer())
                .seller(request.getSeller())
                .build());
    }
}
