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
import com.epam.cme.mdp3.sbe.SBEUtil;
import com.epam.cme.mdp3.sbe.message.AbstractFieldSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// implementation should be complete
public class TradeHandler extends AbstractMktDataHandler {

    private static final Logger logger = LoggerFactory.getLogger(TradeHandler.class);

    private final TradeSummaryImpl summary = new TradeSummaryImpl();

    public TradeHandler(final ChannelContext channelContext, final int securityId, final int subscriptionFlags) {
        super(channelContext, securityId, subscriptionFlags);
    }

    public void updateTradeSummary(final FieldSet tradeEntry, long triggerTime, long transactTime) {

        try {
            double price = SBEUtil.doubleField(tradeEntry, 270);
            long qty;
            int schemaId = tradeEntry.getSchemaId();
            if (schemaId == 65) {
                // long quantities
                qty = SBEUtil.uInt64Field(tradeEntry, 271);
            } else {
                qty = SBEUtil.int32Field(tradeEntry, 271);
            }
            int securityId = SBEUtil.int32Field(tradeEntry, 48);
            int tradeId = SBEUtil.int32Field(tradeEntry, 37711);
            short sideVal = SBEUtil.uInt8Field(tradeEntry, 5797);
            AggressorSide side = AggressorSide.getByCode(sideVal);

            summary.update(securityId, tradeId, price, qty, side, triggerTime, transactTime);

            channelContext.notifyTradeListeners(securityId, summary);
        } catch (Throwable ex) {
            String dump = "";
            if (tradeEntry instanceof AbstractFieldSet) {
                dump = SBEUtil.dumpAllFields((AbstractFieldSet) tradeEntry, "TradeSummary");
            }
            logger.error("Error while updating trade summary entry: {}", dump, ex);
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