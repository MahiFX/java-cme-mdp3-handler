package com.epam.cme.mdp3.core.channel;

import com.epam.cme.mdp3.MdpChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A channel schedules recurring work on the scheduler it is given: the feed idle check when it is built, and a
 * statistics task for every feed it starts. The scheduler usually outlives the channel, so anything left scheduled
 * after close() keeps running against closed workers and keeps them reachable. A client that builds a fresh channel
 * on every reconnect or start-up retry then accumulates them for the life of the process.
 */
public class MdpChannelCloseCancelsTasksTest {
    private ScheduledThreadPoolExecutor scheduler;

    @Before
    public void setUp() {
        scheduler = new ScheduledThreadPoolExecutor(1);
        scheduler.setRemoveOnCancelPolicy(true);
    }

    @After
    public void tearDown() {
        scheduler.shutdownNow();
    }

    private MdpChannel channel() throws Exception {
        final URI config = getClass().getResource("/config.xml").toURI();
        final URI schema = getClass().getResource("/templates_FixBinary.xml").toURI();
        return new MdpChannelBuilder("901", config, schema).usingScheduler(scheduler).build();
    }

    /**
     * A worker only accepts shutdown once its own thread has marked it ACTIVE. Closing a channel in the moment between
     * starting a feed and that thread getting going used to skip the shutdown and then join the thread for ever, while
     * the feed ran on. Repeated so the window is hit; the timeout turns a hang into a failure.
     */
    @Test(timeout = 30_000)
    public void closeStraightAfterStartingAFeedDoesNotHang() throws Exception {
        for (int i = 0; i < 50; i++) {
            final MdpChannel channel = channel();
            channel.startIncrementalFeedA();
            channel.close();
        }
    }

    /** A feed started once the channel is closing must not leave a statistics task behind. */
    @Test
    public void aFeedStartedAfterCloseLeavesNoStatisticsTask() throws Exception {
        final MdpChannel channel = channel();
        channel.close();

        channel.startIncrementalFeedA();
        try {
            assertTrue("nothing scheduled survives a closed channel: " + scheduler.getQueue(), scheduler.getQueue().isEmpty());
        } finally {
            channel.close();
        }
    }

    @Test
    public void closeCancelsTheIdleCheck() throws Exception {
        final MdpChannel channel = channel();
        assertEquals("the idle check is scheduled when the channel is built", 1, scheduler.getQueue().size());

        channel.close();

        assertTrue("nothing the channel scheduled survives close: " + scheduler.getQueue(), scheduler.getQueue().isEmpty());
    }

    @Test
    public void closeCancelsTheStatisticsTaskOfEveryStartedFeed() throws Exception {
        final MdpChannel channel = channel();
        // The test config carries feeds A and B for the incremental feed only; every feed type schedules the same way.
        channel.startIncrementalFeedA();
        channel.startIncrementalFeedB();
        assertEquals("idle check plus one statistics task per started feed", 3, scheduler.getQueue().size());

        channel.close();

        assertTrue("nothing the channel scheduled survives close: " + scheduler.getQueue(), scheduler.getQueue().isEmpty());
    }
}
