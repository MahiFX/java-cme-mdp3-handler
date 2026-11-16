package com.epam.cme.mdp3.core.channel;

import com.epam.cme.mdp3.Feed;
import com.epam.cme.mdp3.FeedType;
import com.epam.cme.mdp3.MdpChannel;
import com.epam.cme.mdp3.VoidChannelListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    @Test
    public void closeCancelsTheIdleCheck() throws Exception {
        final MdpChannel channel = channel();
        assertEquals("the idle check is scheduled when the channel is built", 1, scheduler.getQueue().size());

        channel.close();

        assertTrue("nothing the channel scheduled survives close: " + scheduler.getQueue(), scheduler.getQueue().isEmpty());
    }

    @Test
    public void closeCancelsTheStatisticsTaskOfEveryStartedFeed() throws Exception {
        final CountDownLatch feedsSettled = new CountDownLatch(2);
        final MdpChannel channel = new MdpChannelBuilder("901", getClass().getResource("/config.xml").toURI(),
                getClass().getResource("/templates_FixBinary.xml").toURI())
                .usingScheduler(scheduler)
                .usingListener(new VoidChannelListener() {
                    @Override
                    public void onFeedStarted(final String channelId, final FeedType feedType, final Feed feed) {
                        feedsSettled.countDown();
                    }
                })
                .build();
        // The test config carries feeds A and B for the incremental feed only; every feed type schedules the same way.
        channel.startIncrementalFeedA();
        channel.startIncrementalFeedB();
        assertEquals("idle check plus one statistics task per started feed", 3, scheduler.getQueue().size());

        // A worker only accepts shutdown once its own thread has marked it active, so closing in that window leaves
        // the thread running and close() waiting on it. Let both feeds open first; a feed that cannot open on this
        // host exits by itself, so the wait is bounded rather than required.
        feedsSettled.await(5, TimeUnit.SECONDS);
        channel.close();

        assertTrue("nothing the channel scheduled survives close: " + scheduler.getQueue(), scheduler.getQueue().isEmpty());
    }
}
