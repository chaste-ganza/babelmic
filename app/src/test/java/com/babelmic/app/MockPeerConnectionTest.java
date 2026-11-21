package com.babelmic.app;

import com.babelmic.app.mock.MockPeerConnection;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Tests the peer-to-peer connection practice component. */
public class MockPeerConnectionTest {

    @Test
    public void startsDisconnected() {
        assertFalse(new MockPeerConnection().isConnected());
    }

    @Test
    public void connectsAndTracksState() {
        MockPeerConnection connection = new MockPeerConnection();

        assertTrue(connection.connect("Bob-Pixel16"));
        assertTrue(connection.isConnected());
    }

    @Test
    public void cannotSendBeforeConnecting() {
        MockPeerConnection connection = new MockPeerConnection();
        try {
            connection.sendData("Bonjour");
            fail("Expected IllegalStateException when sending without a connection");
        } catch (IllegalStateException expected) {
            // Expected behaviour.
        }
    }

    @Test
    public void canSendAfterConnecting() {
        MockPeerConnection connection = new MockPeerConnection();
        connection.connect("Bob-Pixel16");

        connection.sendData("Ou est la gare?");
        assertTrue(connection.isConnected());
    }

    @Test
    public void cannotSendAfterDisconnecting() {
        MockPeerConnection connection = new MockPeerConnection();
        connection.connect("Bob-Pixel16");
        connection.disconnect();
        assertFalse(connection.isConnected());

        try {
            connection.sendData("Bonjour");
            fail("Expected IllegalStateException after disconnect");
        } catch (IllegalStateException expected) {
            // Expected behaviour.
        }
    }

    @Test
    public void emptyDeviceIdIsRejected() {
        MockPeerConnection connection = new MockPeerConnection();
        try {
            connection.connect("  ");
            fail("Expected IllegalArgumentException for an empty device id");
        } catch (IllegalArgumentException expected) {
            // Expected behaviour.
        }
    }
}
