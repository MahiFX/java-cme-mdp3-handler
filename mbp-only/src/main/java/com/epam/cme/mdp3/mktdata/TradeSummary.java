package com.epam.cme.mdp3.mktdata;

public interface TradeSummary {
    int getSecurityId();

    int getTradeId();

    double getPrice();

    long getQuantity();

    AggressorSide getSide();

    long getTriggerTime();

    long getTransactTime();
}
