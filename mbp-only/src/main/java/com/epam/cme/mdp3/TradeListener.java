package com.epam.cme.mdp3;

import com.epam.cme.mdp3.mktdata.TradeSummary;

public interface TradeListener {
    void onTradeSummary(String channelId, int securityId, TradeSummary trade);

    void onEndOfIncrement(String channelId, int securityId);
}
