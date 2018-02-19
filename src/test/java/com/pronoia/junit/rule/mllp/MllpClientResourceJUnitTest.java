/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pronoia.junit.rule.mllp;

import com.pronoia.test.util.tcp.SimpleTcpServer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Simple test to verify JUnit is correctly starting/stopping the MLLP Client
 */
public class MllpClientResourceJUnitTest {

    static SimpleTcpServer tcpServer;

    @Rule
    public MllpClientResource mllpClient = new MllpClientResource(tcpServer.getPort());

    @BeforeClass
    public static void setUpClass() throws Exception {
        tcpServer = new SimpleTcpServer("dummy-server").start();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        tcpServer.stop();
    }

    @Test
    public void testConnection() throws Exception {
        assertTrue("Client should be connected", mllpClient.isConnected());
    }

}