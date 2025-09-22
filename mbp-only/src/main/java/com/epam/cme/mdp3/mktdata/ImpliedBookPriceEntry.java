/*
 * Copyright 2004-2016 EPAM Systems
 * This file is part of Java Market Data Handler for CME Market Data (MDP 3.0).
 * Java Market Data Handler for CME Market Data (MDP 3.0) is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Java Market Data Handler for CME Market Data (MDP 3.0) is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Java Market Data Handler for CME Market Data (MDP 3.0).
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.epam.cme.mdp3.mktdata;

import com.epam.cme.mdp3.FieldSet;

public class ImpliedBookPriceEntry implements ImpliedBookPriceLevel {
    protected int qty;
    protected final Price price = new Price();
    protected long triggerTime;
    private long transactTime;

    @Override
    public int getQuantity() {
        return qty;
    }

    @Override
    public Price getPrice() {
        return price;
    }

    @Override
    public long getTriggerTime() {
        return triggerTime;
    }

    public long getTransactTime() {
        return transactTime;
    }

    public void clear() {
        qty = 0;
        price.setNull();
    }

    public void refreshFromAnotherEntry(final ImpliedBookPriceEntry bookEntry) {
        this.qty = bookEntry.qty;
        this.price.setMantissa(bookEntry.getPrice().getMantissa());
        this.triggerTime = bookEntry.triggerTime;
        this.transactTime = bookEntry.transactTime;
    }

    public void refreshFromMessage(final FieldSet fieldSet, long triggerTime, long transactTime) {
        int newQty = fieldSet.getInt32(271);
        long newPrice = fieldSet.getInt64(270);
        boolean changed = newQty != qty || newPrice != price.getMantissa();
        if (changed) {
            this.qty = newQty;
            this.price.setMantissa(newPrice);
            this.triggerTime = triggerTime;
            this.transactTime = transactTime;
        }
    }
}
