package com.epam.cme.mdp3.core.channel;

import com.epam.cme.mdp3.Feed;
import com.epam.cme.mdp3.FeedType;
import com.epam.cme.mdp3.core.cfg.ConnectionCfg;
import com.epam.cme.mdp3.core.cfg.TransportProtocol;
import org.junit.Test;

import java.lang.reflect.Field;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A feed whose open() fails after allocating its socket used to return from run() with the socket and selector still
 * open and its state still ACTIVE. Nothing closed them (the channel only closes workers whose thread is alive), and a
 * worker stuck ACTIVE with no thread can never be stopped or started again. A client that retries such a start leaked
 * one socket and selector per attempt.
 */
public class MdpFeedWorkerOpenFailureTest {

    /** 127.0.0.1 is not a multicast group, so open() binds the socket and opens the selector, then the join throws. */
    private static MdpFeedWorker workerThatFailsToJoin() throws Exception {
        ConnectionCfg cfg = new ConnectionCfg(Feed.A, "test", FeedType.N, TransportProtocol.UDP, "127.0.0.1", Collections.emptyList(), 0);
        return new MdpFeedWorker(cfg);
    }

    private static Object field(final MdpFeedWorker worker, final String name) throws Exception {
        Field f = MdpFeedWorker.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(worker);
    }

    @Test
    public void aFailedOpenReleasesItsSocketAndSelector() throws Exception {
        MdpFeedWorker worker = workerThatFailsToJoin();

        worker.run();

        DatagramChannel socket = (DatagramChannel) field(worker, "multicastChannel");
        Selector selector = (Selector) field(worker, "selector");
        assertTrue("the failure happened after the socket was opened, so this test exercises the leak", socket != null);
        assertFalse("socket released", socket.isOpen());
        assertFalse("selector released", selector != null && selector.isOpen());
    }

    @Test
    public void aFailedOpenLeavesTheWorkerStoppedSoItCanBeStartedAgain() throws Exception {
        MdpFeedWorker worker = workerThatFailsToJoin();

        worker.run();

        assertFalse("no thread is running it, so it must not report ACTIVE", worker.isActive());
        assertTrue("and it can be made runnable again", worker.isRunnable());
    }

    /**
     * A channel closing a feed whose thread has not yet run marks the worker terminated, since its shutdown request is
     * ignored until the worker is ACTIVE. When that thread finally runs it must not open anything or stay ACTIVE.
     */
    @Test
    public void aTerminatedWorkerDoesNotOpenWhenItsThreadFinallyRuns() throws Exception {
        MdpFeedWorker worker = workerThatFailsToJoin();

        worker.terminate();
        worker.run();

        assertTrue("it never got as far as opening a socket", field(worker, "multicastChannel") == null);
        assertFalse(worker.isActive());
    }

    /** A start request on a terminated worker must not cancel its shutdown and bring it back. */
    @Test
    public void aTerminatedWorkerCannotBeRevivedByCancellingItsShutdown() throws Exception {
        MdpFeedWorker worker = workerThatFailsToJoin();
        assertTrue("made ACTIVE as a running worker would be", worker.isRunnable());

        worker.terminate();

        assertFalse("cancelling the shutdown is refused", worker.cancelShutdownIfStarted());
        assertFalse(worker.isActive());
    }

    /** close() on a worker that never opened (or failed part-way) must not throw. */
    @Test
    public void closeIsSafeOnAWorkerThatNeverOpened() throws Exception {
        workerThatFailsToJoin().close();
    }
}
