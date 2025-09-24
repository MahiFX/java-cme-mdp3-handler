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
import com.epam.cme.mdp3.core.channel.ChannelContext;
import com.epam.cme.mdp3.sbe.message.SbeDouble;

// implementation should be complete
public class TradeHandler extends AbstractMktDataHandler {

    public static final int NEW_ACTION = 0;

    private final TradeSummaryImpl summary = new TradeSummaryImpl();

    public TradeHandler(final ChannelContext channelContext, final int securityId, final int subscriptionFlags) {
        super(channelContext, securityId, subscriptionFlags);
    }

    public void updateTradeSummary(final FieldSet tradeEntry, long triggerTime, long transactTime) {

        byte action = tradeEntry.getInt8(279);
        if (action == NEW_ACTION) {
            SbeDouble price = SbeDouble.instance();
            tradeEntry.getDouble(270, price);
            if (price.isNull()) {
                return;
            }
            long qty;
            int schemaId = tradeEntry.getSchemaId();
            if (schemaId == 65) {
                // long quantities
                qty = tradeEntry.getUInt64(271);
            } else {
                qty = tradeEntry.getInt32(271);
            }
            int securityId = tradeEntry.getInt32(48);
            int tradeId = tradeEntry.getInt32(37711);
            AggressorSide side = AggressorSide.getByCode(tradeEntry.getUInt8(5797));

            summary.update(securityId, tradeId, price.asDouble(), qty, side, triggerTime, transactTime);

            channelContext.notifyTradeListeners(securityId, summary);
        }
    }


    public void updateElectronicVolume(final FieldSet incrementEntry) {

    }


    @Override
    public void clear() {
        summary.reset();
    }

    public void commitEvent() {
        // TODO batch trades
    }
}