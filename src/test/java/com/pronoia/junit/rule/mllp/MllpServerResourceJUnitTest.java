package com.pronoia.junit.rule.mllp;

import com.pronoia.test.util.tcp.SimpleTcpClient;
import org.junit.Rule;
import org.junit.Test;

import javax.annotation.Resource;

import static org.junit.Assert.*;

public class MllpServerResourceJUnitTest {

    @Rule
    public MllpServerResource mllpServer = new MllpServerResource();

    @Test
    public void testConnection() throws Exception {
        SimpleTcpClient client = new SimpleTcpClient().port(mllpServer.getListenPort()).start();

        assertTrue(mllpServer.isActive());
        assertNotEquals(0, mllpServer.getListenPort());
        assertTrue(client.isConnected());
    }

}