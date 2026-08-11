package com.epam.cme.mdp3.core.control;

import com.epam.cme.mdp3.ChannelState;
import com.epam.cme.mdp3.core.channel.ChannelContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * A channel is INITIAL until its snapshot recovery completes, and recovery can complete two ways:
 * <ul>
 *   <li>a snapshot packet with sequence 1 arrives while {@code canStopSnapshotListening()} — handled by
 *       {@code handleSnapshotMessage}, which calls {@code stopSnapshotListening()} and promotes the state;</li>
 *   <li>every out-of-sync instrument recovers individually, so the last {@code removeOutOfSyncInstrument}
 *       leaves nothing left to recover.</li>
 * </ul>
 * The second path used to stop the snapshot feeds without promoting the state, so a channel that recovered
 * that way stayed INITIAL for the rest of the session while publishing perfectly good data. Callers reading
 * the channel state saw a permanently unhealthy market.
 * <p>
 * Which ending a channel gets is a race against its own instrument recoveries, so two channels in the same
 * process can behave differently on the same run: one reaches SYNC while the other stays INITIAL for hours,
 * publishing an equally good book throughout.
 */
public class ChannelControllerSyncStateTest {

    /** Absorbs the channel callbacks; the controller's own state is what these tests assert. */
    private static final class StubContext extends ChannelContext {
        private StubContext() {
            super(null, null, 0);
        }

        @Override
        public void stopSnapshotFeeds() {
            // no channel behind this stub
        }

        @Override
        public boolean hasMdListeners() {
            return false;
        }

        @Override
        public void notifyChannelStateListeners(final ChannelState prevState, final ChannelState newState) {
            // no channel behind this stub; the transition itself is what the tests assert
        }
    }

    private final StubContext context = new StubContext();
    private final ChannelController controller = new ChannelController(context);

    @Test
    public void aNewChannelStartsInitial() {
        assertEquals(ChannelState.INITIAL, controller.getState());
    }

    /**
     * The regression. Every instrument recovers, so nothing is left out of sync: recovery IS complete and the
     * channel must say so, without waiting for a sequence-1 snapshot packet that may never come.
     */
    @Test
    public void recoveringTheLastOutOfSyncInstrumentSyncsTheChannel() {
        controller.addOutOfSyncInstrument(1);
        controller.addOutOfSyncInstrument(2);

        controller.removeOutOfSyncInstrument(1);
        assertEquals("one instrument still recovering, so the channel is not synced yet",
                     ChannelState.INITIAL, controller.getState());

        controller.removeOutOfSyncInstrument(2);
        assertEquals("nothing left to recover, so the channel is synced",
                     ChannelState.SYNC, controller.getState());
    }

    /** Removing something that was never out of sync must not fabricate a sync. */
    @Test
    public void removingAnUnknownInstrumentDoesNotSyncTheChannel() {
        controller.addOutOfSyncInstrument(1);

        controller.removeOutOfSyncInstrument(99);

        assertEquals(ChannelState.INITIAL, controller.getState());
    }

    /** A closing channel must not be dragged back to SYNC by a late instrument recovery. */
    @Test
    public void aClosingChannelIsNotPromoted() {
        controller.addOutOfSyncInstrument(1);
        controller.switchState(ChannelState.CLOSING);

        controller.removeOutOfSyncInstrument(1);

        assertEquals(ChannelState.CLOSING, controller.getState());
    }

    /** Already SYNC is left alone rather than re-fired, so listeners do not see a spurious transition. */
    @Test
    public void anAlreadySyncedChannelIsNotSwitchedAgain() {
        controller.switchState(ChannelState.SYNC);
        controller.addOutOfSyncInstrument(1);

        controller.removeOutOfSyncInstrument(1);

        assertEquals(ChannelState.SYNC, controller.getState());
    }
}
