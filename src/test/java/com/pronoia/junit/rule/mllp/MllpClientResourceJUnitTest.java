package com.pronoia.junit.rule.mllp;

import com.pronoia.test.util.tcp.SimpleTcpServer;
import org.junit.*;

import javax.annotation.Resource;

import static org.junit.Assert.*;

/**
 * Simple test to verify JUnit is correctly starting/stopping the MLLP Client
 */
public class MllpClientResourceJUnitTest {

    static SimpleTcpServer tcpServer;

    @Rule
    public MllpClientResource mllpClient = new MllpClientResource(tcpServer.getPort());

    @BeforeClass
    static public void setUpClass() throws Exception {
        tcpServer = new SimpleTcpServer("dummy-server").start();
    }

    @AfterClass
    static public void tearDownClass() throws Exception {
        tcpServer.stop();
    }

    @Test
    public void testConnection() throws Exception {
        assertTrue("Client should be connected", mllpClient.isConnected());
    }

}