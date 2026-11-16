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

    /** close() on a worker that never opened (or failed part-way) must not throw. */
    @Test
    public void closeIsSafeOnAWorkerThatNeverOpened() throws Exception {
        workerThatFailsToJoin().close();
    }
}
