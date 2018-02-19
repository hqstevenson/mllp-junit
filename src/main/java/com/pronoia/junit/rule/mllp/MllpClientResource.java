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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MllpClientResource extends ExternalResource {
    public static final Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;

    static final char START_OF_BLOCK = 0x0b;
    static final char END_OF_BLOCK = 0x1c;
    static final char END_OF_DATA = 0x0d;
    static final int END_OF_STREAM = -1;

    Logger log = LoggerFactory.getLogger(this.getClass());

    Socket clientSocket;
    InputStream inputStream;
    OutputStream outputStream;

    String mllpHost = "0.0.0.0";
    int mllpPort = -1;

    boolean sendStartOfBlock = true;
    boolean sendEndOfBlock = true;
    boolean sendEndOfData = true;

    int connectTimeout = 5000;
    int soTimeout = 5000;
    boolean reuseAddress;
    boolean tcpNoDelay = true;

    boolean lazyConnect;


    /**
     * Use this constructor to avoid having the connection started by JUnit (since the port is still -1)
     */
    public MllpClientResource() {

    }

    public MllpClientResource(int port) {
        this.mllpPort = port;
    }

    public MllpClientResource(int port, boolean lazyConnect) {
        this.mllpPort = port;
        this.lazyConnect = lazyConnect;
    }

    public MllpClientResource(String host, int port) {
        this.mllpHost = host;
        this.mllpPort = port;
    }

    public MllpClientResource(String host, int port, boolean lazyConnect) {
        this.mllpHost = host;
        this.mllpPort = port;
        this.lazyConnect = lazyConnect;
    }

    @Override
    protected void before() throws Throwable {
        if (0 < mllpPort && !lazyConnect) {
            this.connect();
        }

        super.before();
    }

    @Override
    protected void after() {
        super.after();
        this.disconnect();
    }

    public void connect() {
        this.connect(this.connectTimeout);
    }

    public void connect(int connectTimeout) {
        try {
            clientSocket = new Socket();

            clientSocket.connect(new InetSocketAddress(mllpHost, mllpPort), connectTimeout);

            clientSocket.setSoTimeout(soTimeout);
            clientSocket.setSoLinger(false, -1);
            clientSocket.setReuseAddress(reuseAddress);
            clientSocket.setTcpNoDelay(tcpNoDelay);

            inputStream = clientSocket.getInputStream();
            outputStream = new BufferedOutputStream(clientSocket.getOutputStream(), 2048);
        } catch (IOException e) {
            String errorMessage = String.format("Unable to establish connection to %s:%s", mllpHost, mllpPort);
            log.error(errorMessage, e);
            throw new MllpJUnitResourceException(errorMessage, e);
        }
    }

    public void close() {
        this.disconnect();
        return;
    }

    public void reset() {
        try {
            clientSocket.setSoLinger(true, 0);
        } catch (SocketException socketEx) {
            log.warn("Exception encountered setting set SO_LINGER to force a TCP reset", socketEx);
        }
        this.disconnect();
        return;
    }

    public void disconnect() {
        try {
            if (null != clientSocket && null != inputStream) {
                clientSocket.close();
            }
        } catch (IOException e) {
            log.warn(String.format("Exception encountered closing connection to {}:{}", mllpHost, mllpPort), e);
        } finally {
            inputStream = null;
            outputStream = null;
            clientSocket = null;
        }
    }

    public boolean isConnected() {
        if (null == clientSocket) {
            return false;
        }

        return clientSocket.isConnected() && !clientSocket.isClosed();
    }

    public void sendData(byte[] byteData) {
        boolean disconnectAfterSend = false;
        this.sendData(byteData, disconnectAfterSend);
    }

    public void sendData(byte[] byteData, boolean disconnectAfterSend) {
        if (null == clientSocket) {
            this.connect();
        }

        if (!isConnected()) {
            throw new MllpJUnitResourceException("Cannot send message - client is not connected");
        }

        try {
            outputStream.write(byteData, 0, byteData.length);
        } catch (IOException e) {
            log.error("Unable to send raw string", e);
            throw new MllpJUnitResourceException("Unable to send raw string", e);
        }

        if (disconnectAfterSend) {
            log.warn("Closing TCP connection");
            disconnect();
        }
    }

    public void sendData(String data) {
        boolean disconnectAfterSend = false;
        this.sendData(data, disconnectAfterSend);
    }

    public void sendData(String data, boolean disconnectAfterSend) {
        sendData(data, disconnectAfterSend, DEFAULT_CHARSET);
    }

    public void sendData(String data, Charset charset) {
        boolean disconnectAfterSend = false;
        this.sendData(data, disconnectAfterSend, charset);
    }

    public void sendData(String data, boolean disconnectAfterSend, Charset charset) {
        byte[] payloadBytes = data.getBytes(charset);

        this.sendData(payloadBytes, disconnectAfterSend);
    }

    public void sendFramedData(byte[] hl7Bytes) {
        boolean disconnectAfterSend = false;
        this.sendFramedData(hl7Bytes, disconnectAfterSend);
    }

    public void sendFramedData(byte[] hl7Bytes, boolean disconnectAfterSend) {
        if (null == clientSocket) {
            this.connect();
        }

        if (!isConnected()) {
            throw new MllpJUnitResourceException("Cannot send message - client is not connected");
        }
        if (null == outputStream) {
            throw new MllpJUnitResourceException("Cannot send message - output stream is null");
        }
        try {
            if (sendStartOfBlock) {
                outputStream.write(START_OF_BLOCK);
            } else {
                log.warn("Not sending START_OF_BLOCK");
            }
            outputStream.write(hl7Bytes, 0, hl7Bytes.length);
            if (sendEndOfBlock) {
                outputStream.write(END_OF_BLOCK);
            } else {
                log.warn("Not sending END_OF_BLOCK");
            }
            if (sendEndOfData) {
                outputStream.write(END_OF_DATA);
            } else {
                log.warn("Not sending END_OF_DATA");
            }
            outputStream.flush();
        } catch (IOException e) {
            log.error("Unable to send HL7 message", e);
            throw new MllpJUnitResourceException("Unable to send HL7 message", e);
        }

        if (disconnectAfterSend) {
            log.warn("Closing TCP connection");
            disconnect();
        }
    }


    public void sendFramedData(String hl7Message) {
        boolean disconnectAfterSend = false;
        this.sendFramedData(hl7Message, disconnectAfterSend);
    }

    public void sendFramedData(String hl7Message, Charset charset) {
        boolean disconnectAfterSend = false;
        this.sendFramedData(hl7Message, disconnectAfterSend, charset);
    }

    public void sendFramedData(String hl7Message, boolean disconnectAfterSend) {
        this.sendFramedData(hl7Message, disconnectAfterSend, DEFAULT_CHARSET);
    }

    public void sendFramedData(String hl7Message, boolean disconnectAfterSend, Charset charset) {
        byte[] hl7Bytes = hl7Message.getBytes(charset);

        this.sendFramedData(hl7Bytes, disconnectAfterSend);
    }

    public void sendFramedDataInMultiplePackets(byte[] hl7Bytes, byte flushByte) {
        boolean disconnectAfterSend = false;
        sendFramedDataInMultiplePackets(hl7Bytes, flushByte, disconnectAfterSend);
    }

    public void sendFramedDataInMultiplePackets(byte[] hl7Bytes, byte flushByte, boolean disconnectAfterSend) {
        if (null == clientSocket) {
            this.connect();
        }

        if (!isConnected()) {
            throw new MllpJUnitResourceException("Cannot send message - client is not connected");
        }
        if (null == outputStream) {
            throw new MllpJUnitResourceException("Cannot send message - output stream is null");
        }
        try {
            if (sendStartOfBlock) {
                outputStream.write(START_OF_BLOCK);
            } else {
                log.warn("Not sending START_OF_BLOCK");
            }
            for (int i = 0; i < hl7Bytes.length; ++i) {
                outputStream.write(hl7Bytes[i]);
                if (flushByte == hl7Bytes[i]) {
                    outputStream.flush();
                }
            }
            if (sendEndOfBlock) {
                outputStream.write(END_OF_BLOCK);
            } else {
                log.warn("Not sending END_OF_BLOCK");
            }
            if (sendEndOfData) {
                outputStream.write(END_OF_DATA);
            } else {
                log.warn("Not sending END_OF_DATA");
            }
            outputStream.flush();
        } catch (IOException e) {
            log.error("Unable to send HL7 message", e);
            throw new MllpJUnitResourceException("Unable to send HL7 message", e);
        }

        if (disconnectAfterSend) {
            log.warn("Closing TCP connection");
            disconnect();
        }
    }

    public void sendFramedDataInMultiplePackets(String hl7Message, byte flushByte) {
        boolean disconnectAfterSend = false;
        sendFramedDataInMultiplePackets(hl7Message, flushByte, disconnectAfterSend);
    }

    public void sendFramedDataInMultiplePackets(String hl7Message, byte flushByte, Charset charset) {
        boolean disconnectAfterSend = false;
        sendFramedDataInMultiplePackets(hl7Message, flushByte, disconnectAfterSend, charset);
    }

    public void sendFramedDataInMultiplePackets(String hl7Message, byte flushByte, boolean disconnectAfterSend) {
        this.sendFramedDataInMultiplePackets(hl7Message, flushByte, disconnectAfterSend, DEFAULT_CHARSET);
    }

    public void sendFramedDataInMultiplePackets(String hl7Message, byte flushByte, boolean disconnectAfterSend, Charset charset) {
        byte[] hl7Bytes = hl7Message.getBytes(charset);

        this.sendFramedDataInMultiplePackets(hl7Bytes, flushByte, disconnectAfterSend);
    }

    public byte[] receiveFramedBytes() throws SocketException, SocketTimeoutException {
        return receiveFramedBytes(soTimeout);
    }

    public byte[] receiveFramedBytes(int timeout) throws SocketException, SocketTimeoutException {
        if (!isConnected()) {
            throw new MllpJUnitResourceException("Cannot receive acknowledgement - client is not connected");
        }
        if (null == outputStream) {
            throw new MllpJUnitResourceException("Cannot receive acknowledgement - output stream is null");
        }

        clientSocket.setSoTimeout(timeout);
        ByteArrayOutputStream receivedBytes = new ByteArrayOutputStream();
        try {
            int firstByte = inputStream.read();
            if (START_OF_BLOCK != firstByte) {
                if (isConnected()) {
                    if (END_OF_STREAM == firstByte) {
                        log.warn("END_OF_STREAM reached while waiting for START_OF_BLOCK - closing socket");
                        try {
                            clientSocket.close();
                        } catch (Exception ex) {
                            log.warn("Exception encountered closing socket after receiving END_OF_STREAM while waiting for START_OF_BLOCK");
                        }
                        return null;
                    } else {
                        log.error("Acknowledgement did not start with START_OF_BLOCK: {}", firstByte);
                        throw new MllpJUnitResourceCorruptFrameException("Message did not start with START_OF_BLOCK");
                    }
                } else {
                    throw new MllpJUnitResourceException("Connection terminated");
                }
            }
            boolean readingMessage = true;
            while (readingMessage) {
                int nextByte = inputStream.read();
                switch (nextByte) {
                case -1:
                    throw new MllpJUnitResourceCorruptFrameException("Reached end of stream before END_OF_BLOCK");
                case START_OF_BLOCK:
                    throw new MllpJUnitResourceCorruptFrameException("Received START_OF_BLOCK before END_OF_BLOCK");
                case END_OF_BLOCK:
                    if (END_OF_DATA != inputStream.read()) {
                        throw new MllpJUnitResourceCorruptFrameException("END_OF_BLOCK was not followed by END_OF_DATA");
                    }
                    readingMessage = false;
                    break;
                default:
                    receivedBytes.write(nextByte);
                }
            }
        } catch (SocketTimeoutException timeoutEx) {
            if (0 < receivedBytes.size()) {
                log.error("Timeout waiting for acknowledgement", timeoutEx);
            } else {
                log.error("Timeout while reading acknowledgement\n" + receivedBytes.toString().replace('\r', '\n'), timeoutEx);
            }
            throw new MllpJUnitResourceTimeoutException("Timeout while reading acknowledgement", timeoutEx);
        } catch (IOException e) {
            log.error("Unable to read HL7 acknowledgement", e);
            throw new MllpJUnitResourceException("Unable to read HL7 acknowledgement", e);
        }

        return receivedBytes.toByteArray();
    }

    public String receiveFramedData() throws SocketException, SocketTimeoutException {
        return receiveFramedData(soTimeout);
    }

    public String receiveFramedData(Charset charset) throws SocketException, SocketTimeoutException {
        return receiveFramedData(soTimeout, charset);
    }

    public String receiveFramedData(int timeout) throws SocketException, SocketTimeoutException {
        return receiveFramedData(timeout, DEFAULT_CHARSET);
    }

    public String receiveFramedData(int timeout, Charset charset) throws SocketException, SocketTimeoutException {
        byte[] receivedBytes = receiveFramedBytes(timeout);

        if (receivedBytes != null) {
            if (receivedBytes.length > 0) {
                return new String(receivedBytes, charset);
            } else {
                return "";
            }
        }

        return null;
    }

    public byte[] receiveBytes() throws SocketException, SocketTimeoutException {
        return receiveBytes(soTimeout);
    }

    public byte[] receiveBytes(int timeout) throws SocketException, SocketTimeoutException {
        clientSocket.setSoTimeout(timeout);

        ByteArrayOutputStream availableInput = new ByteArrayOutputStream();

        try {
            do {
                availableInput.write(inputStream.read());
            } while (0 < inputStream.available());
        } catch (SocketTimeoutException timeoutEx) {
            log.error("Timeout while receiving available input", timeoutEx);
            throw new MllpJUnitResourceTimeoutException("Timeout while receiving available input", timeoutEx);
        } catch (IOException e) {
            log.warn("Exception encountered eating available input", e);
            throw new MllpJUnitResourceException("Exception encountered eating available input", e);
        }

        return availableInput.toByteArray();
    }

    public String receiveData() throws SocketException, SocketTimeoutException {
        return receiveData(soTimeout);
    }

    public String receiveData(Charset charset) throws SocketException, SocketTimeoutException {
        return receiveData(soTimeout, charset);
    }

    public String receiveData(int timeout) throws SocketException, SocketTimeoutException {
        return receiveData(timeout, DEFAULT_CHARSET);
    }

    public String receiveData(int timeout, Charset charset) throws SocketException, SocketTimeoutException {
        clientSocket.setSoTimeout(timeout);

        byte[] receivedBytes = this.receiveBytes(timeout);

        if (receivedBytes != null) {
            if (receivedBytes.length > 0) {
                return new String(receivedBytes, charset);
            } else {
                return "";
            }
        }

        return null;
    }

    public byte[] eatBytes() throws SocketException, SocketTimeoutException {
        return eatBytes(soTimeout);
    }

    public byte[] eatBytes(int timeout) throws SocketException {
        clientSocket.setSoTimeout(timeout);

        ByteArrayOutputStream availableInput = new ByteArrayOutputStream();
        try {
            while (0 < inputStream.available()) {
                availableInput.write(inputStream.read());
            }
        } catch (IOException e) {
            log.warn("Exception encountered eating available input", e);
            throw new MllpJUnitResourceException("Exception encountered eating available input", e);
        }

        return availableInput.toByteArray();
    }

    public String eatData() throws SocketException, SocketTimeoutException, UnsupportedEncodingException {
        return eatData(soTimeout);
    }

    public String eatData(int timeout) throws SocketException, UnsupportedEncodingException {
        return eatData(timeout, DEFAULT_CHARSET);
    }

    public String eatData(int timeout, Charset charset) throws SocketException, UnsupportedEncodingException {
        byte[] eatenBytes = this.eatBytes(timeout);

        if (eatenBytes != null) {
            if (eatenBytes.length > 0) {
                return new String(eatenBytes, charset);
            } else {
                return "";
            }
        }

        return null;
    }

    public String sendFramedDataAndWaitForAcknowledgement(byte[] hl7Bytes) throws SocketException, SocketTimeoutException {
        sendFramedData(hl7Bytes);
        return receiveFramedData();
    }

    public String sendFramedDataAndWaitForAcknowledgement(byte[] hl7Bytes, int acknwoledgementTimeout) throws SocketException, SocketTimeoutException {
        sendFramedData(hl7Bytes);
        return receiveFramedData(acknwoledgementTimeout);
    }

    public String sendFramedDataAndWaitForAcknowledgement(String hl7Data) throws SocketException, SocketTimeoutException {
        sendFramedData(hl7Data);
        return receiveFramedData();
    }

    public String sendFramedDataAndWaitForAcknowledgement(String hl7Data, Charset charset) throws SocketException, SocketTimeoutException {
        sendFramedData(hl7Data, charset);
        return receiveFramedData();
    }

    public String sendFramedDataAndWaitForAcknowledgement(String hl7Data, int acknwoledgementTimeout) throws SocketException, SocketTimeoutException {
        sendFramedData(hl7Data);
        return receiveFramedData(acknwoledgementTimeout);
    }

    public String sendFramedDataAndWaitForAcknowledgement(String hl7Data, int acknwoledgementTimeout, Charset charset) throws SocketException, SocketTimeoutException {
        sendFramedData(hl7Data, charset);
        return receiveFramedData(acknwoledgementTimeout);
    }

    public String getMllpHost() {
        return mllpHost;
    }

    public void setMllpHost(String mllpHost) {
        this.mllpHost = mllpHost;
    }

    public int getMllpPort() {
        return mllpPort;
    }

    public void setMllpPort(int mllpPort) {
        this.mllpPort = mllpPort;
    }

    public boolean isSendStartOfBlock() {
        return sendStartOfBlock;
    }

    public void setSendStartOfBlock(boolean sendStartOfBlock) {
        this.sendStartOfBlock = sendStartOfBlock;
    }

    public boolean isSendEndOfBlock() {
        return sendEndOfBlock;
    }

    public void setSendEndOfBlock(boolean sendEndOfBlock) {
        this.sendEndOfBlock = sendEndOfBlock;
    }

    public boolean isSendEndOfData() {
        return sendEndOfData;
    }

    public void setSendEndOfData(boolean sendEndOfData) {
        this.sendEndOfData = sendEndOfData;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    public boolean isReuseAddress() {
        return reuseAddress;
    }

    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public boolean isLazyConnect() {
        return lazyConnect;
    }

    public void setLazyConnect(boolean lazyConnect) {
        this.lazyConnect = lazyConnect;
    }
}