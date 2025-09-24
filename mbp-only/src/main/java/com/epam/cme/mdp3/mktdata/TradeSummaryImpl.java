package com.epam.cme.mdp3.mktdata;

public class TradeSummaryImpl implements TradeSummary {
    private int securityId;
    private int tradeId;
    private double price;
    private long quantity;
    private AggressorSide side;
    private long triggerTime;
    private long transactTime;

    public TradeSummaryImpl() {
    }

    public void update(int securityId, int tradeId, double price, long quantity, AggressorSide side, long triggerTime, long transactTime) {
        this.securityId = securityId;
        this.tradeId = tradeId;
        this.price = price;
        this.quantity = quantity;
        this.side = side;
        this.triggerTime = triggerTime;
        this.transactTime = transactTime;
    }

    public void reset() {
        this.securityId = 0;
        this.tradeId = 0;
        this.price = 0.0;
        this.quantity = 0;
        this.side = null;
        this.triggerTime = 0;
        this.transactTime = 0;
    }

    @Override
    public int getSecurityId() {
        return securityId;
    }

    @Override
    public int getTradeId() {
        return tradeId;
    }

    @Override
    public double getPrice() {
        return price;
    }

    @Override
    public long getQuantity() {
        return quantity;
    }

    @Override
    public AggressorSide getSide() {
        return side;
    }

    @Override
    public long getTriggerTime() {
        return triggerTime;
    }

    @Override
    public long getTransactTime() {
        return transactTime;
    }
}
