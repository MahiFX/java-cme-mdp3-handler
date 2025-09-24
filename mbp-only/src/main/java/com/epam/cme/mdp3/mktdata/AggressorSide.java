package com.epam.cme.mdp3.mktdata;

public enum AggressorSide {
    BUY,
    SELL,
    NO_AGGRESSOR;

    public static AggressorSide getByCode(short uInt8) {
        switch (uInt8) {
            case 1:
                return BUY;
            case 2:
                return SELL;
            default:
                return NO_AGGRESSOR;
        }
    }
}
