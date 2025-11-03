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

package com.epam.cme.mdp3.core.channel;

import com.epam.cme.mdp3.ChannelState;
import com.epam.cme.mdp3.core.control.InstrumentController;
import com.epam.cme.mdp3.sbe.schema.MdpMessageTypes;

public class ChannelContext {
    public final MdpChannelImpl channel;
    private final MdpMessageTypes mdpMessageTypes;
    private int gapThreshold;

    public ChannelContext(final MdpChannelImpl channel, final MdpMessageTypes mdpMessageTypes, final int gapThreshold) {
        this.channel = channel;
        this.mdpMessageTypes = mdpMessageTypes;
        this.gapThreshold = gapThreshold;
    }

    public MdpChannelImpl getChannel() {
        return channel;
    }

    public InstrumentController findInstrumentController(final int securityId, final String secDesc) {
        return channel.findController(securityId, secDesc);
    }

    public ChannelInstruments getInstruments() {
        return channel.instruments;
    }

    public MdpMessageTypes getMdpMessageTypes() {
        return mdpMessageTypes;
    }

    public boolean hasMdListeners() {
        return this.getChannel().hasMdListener();
    }

    public boolean isSnapshotFeedsActive() {
        return channel.isSnapshotFeedsActive();
    }

    public int getGapThreshold() {
        return gapThreshold;
    }

    public void setGapThreshold(int gapThreshold) {
        this.gapThreshold = gapThreshold;
    }


    public void stopInstrumentFeeds() {
        channel.stopInstrumentFeedA();
        channel.stopInstrumentFeedB();
    }

    public void stopSnapshotFeeds() {
        channel.stopSnapshotFeeds();
    }

    public void startSnapshotFeeds() {
        channel.startSnapshotFeeds();
    }

    public void subscribeToSnapshotsForInstrument(final Integer securityId) {
        channel.subscribeToSnapshotsForInstrument(securityId);
    }

    public void unsubscribeToSnapshotsForInstrument(final Integer securityId) {
        channel.unsubscribeFromSnapshotsForInstrument(securityId);
    }

    public long getPrcdSeqNum() {
        return getChannel().getController().getPrcdSeqNum();
    }

    public ChannelState getChannelState() {
        return this.channel.getState();
    }

    public int getQueueSlotInitBufferSize() {
        return this.channel.getQueueSlotInitBufferSize();
    }

    public int getIncrQueueSize() {
        return this.channel.getIncrQueueSize();
    }

}
