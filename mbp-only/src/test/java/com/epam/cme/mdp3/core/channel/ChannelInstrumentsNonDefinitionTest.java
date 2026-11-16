package com.epam.cme.mdp3.core.channel;

import com.epam.cme.mdp3.MdpMessage;
import com.epam.cme.mdp3.MdpPacket;
import com.epam.cme.mdp3.sbe.message.SbeMessage;
import com.epam.cme.mdp3.sbe.schema.MdpMessageTypes;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The instrument feed can carry messages that are not security definitions, and a template the loaded schema
 * does not know. Only security definitions are given their message type, so anything else used to reach the
 * definition listeners with no metadata: the first field read threw, the rest of the packet was dropped, and
 * the instrument cycle never counted down, so a client waiting for the definitions to complete timed out.
 */
public class ChannelInstrumentsNonDefinitionTest {
    private static final int CHANNEL_RESET = 4;
    private static final int CHANNEL_RESET_BLOCK = 9;
    private static final int INSTRUMENT_DEFINITION_FUTURE = 54;
    private static final int INSTRUMENT_DEFINITION_FUTURE_BLOCK = 224;
    private static final int UNKNOWN_TEMPLATE = 999;
    private static final int UNDEFINED_TEMPLATE_IN_RANGE = 5;

    /** Records what reaches onMessage rather than notifying a channel, so the routing is what is asserted. */
    private static final class RecordingInstruments extends ChannelInstruments {
        final List<Integer> templates = new ArrayList<>();
        final List<Boolean> typed = new ArrayList<>();

        RecordingInstruments(final ChannelContext context) {
            super(context);
        }

        @Override
        public void onMessage(final MdpFeedContext feedContext, final MdpMessage secDefMsg) {
            templates.add(secDefMsg.getSchemaId());
            typed.add(((SbeMessage) secDefMsg).getMessageType() != null);
        }
    }

    private RecordingInstruments instruments;

    @Before
    public void setUp() throws Exception {
        final MdpMessageTypes types = new MdpMessageTypes(getClass().getResource("/templates_FixBinary.xml").toURI());
        assertNotNull("schema must know the definition template", types.getMessageType(INSTRUMENT_DEFINITION_FUTURE));
        assertNotNull("schema must know the non-definition template", types.getMessageType(CHANNEL_RESET));
        assertNull("the in-range template must be undefined", types.getMessageType(UNDEFINED_TEMPLATE_IN_RANGE));
        instruments = new RecordingInstruments(new ChannelContext(null, types, 0));
    }

    /** Positive control: a definition on its own reaches onMessage with its message type set. */
    @Test
    public void aSecurityDefinitionReachesTheListenerTyped() {
        instruments.onPacket(null, packet(new int[][]{{INSTRUMENT_DEFINITION_FUTURE, INSTRUMENT_DEFINITION_FUTURE_BLOCK}}), 0);

        assertEquals(List.of(INSTRUMENT_DEFINITION_FUTURE), instruments.templates);
        assertEquals(List.of(true), instruments.typed);
    }

    @Test
    public void aNonDefinitionMessageIsSkippedAndTheDefinitionsAfterItStillArrive() {
        instruments.onPacket(null, packet(new int[][]{
                {CHANNEL_RESET, CHANNEL_RESET_BLOCK},
                {INSTRUMENT_DEFINITION_FUTURE, INSTRUMENT_DEFINITION_FUTURE_BLOCK}}), 0);

        assertEquals("only the definition reaches the listener", List.of(INSTRUMENT_DEFINITION_FUTURE), instruments.templates);
        assertTrue("and it is typed", instruments.typed.get(0));
    }

    /** Above the highest template id the schema defines. */
    @Test
    public void aTemplateBeyondTheSchemaIsSkippedRatherThanThrowing() {
        instruments.onPacket(null, packet(new int[][]{
                {UNKNOWN_TEMPLATE, 4},
                {INSTRUMENT_DEFINITION_FUTURE, INSTRUMENT_DEFINITION_FUTURE_BLOCK}}), 0);

        assertEquals(List.of(INSTRUMENT_DEFINITION_FUTURE), instruments.templates);
    }

    /** Inside the schema's id range but not defined by it. */
    @Test
    public void anUndefinedTemplateInsideTheSchemaRangeIsSkipped() {
        instruments.onPacket(null, packet(new int[][]{
                {UNDEFINED_TEMPLATE_IN_RANGE, 4},
                {INSTRUMENT_DEFINITION_FUTURE, INSTRUMENT_DEFINITION_FUTURE_BLOCK}}), 0);

        assertEquals(List.of(INSTRUMENT_DEFINITION_FUTURE), instruments.templates);
    }

    /**
     * Builds an MDP packet: a 12-byte packet header, then per message a 2-byte size (counting itself), the SBE
     * header (block length, template id, schema id, version) and a zeroed body of the given block length. The
     * listener here reads only the header, so the body content does not matter.
     */
    private static MdpPacket packet(final int[][] messages) {
        final ByteBuffer bb = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(1).putLong(0L);
        for (final int[] m : messages) {
            final int template = m[0];
            final int block = m[1];
            bb.putShort((short) (2 + 8 + block));
            bb.putShort((short) block);
            bb.putShort((short) template);
            bb.putShort((short) 1);
            bb.putShort((short) 9);
            bb.put(new byte[block]);
        }
        bb.flip();
        final MdpPacket packet = MdpPacket.allocate();
        packet.wrapFromBuffer(bb);
        return packet;
    }
}
