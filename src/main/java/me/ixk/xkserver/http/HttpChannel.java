/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;
import me.ixk.xkserver.conntecor.Connector;
import me.ixk.xkserver.conntecor.Poller;
import me.ixk.xkserver.http.HttpParser.RequestHandler;

/**
 * HttpChannel
 *
 * @author Otstar Lin
 * @date 2020/10/27 上午 9:26
 */
public class HttpChannel implements RequestHandler {
    private final Poller poller;
    private final SelectionKey selectionKey;
    private final SocketChannel channel;
    private final Socket socket;
    private final Connector connector;
    private final Executor executor;
    private HttpMethod httpMethod;
    private HttpUri httpUri;
    private HttpVersion httpVersion;
    private final HttpFields httpFields = new HttpFields();
    private final HttpInput httpInput = new HttpInput();

    public HttpChannel(
        Connector connector,
        Poller poller,
        SelectionKey selectionKey
    ) {
        this.connector = connector;
        this.poller = poller;
        this.selectionKey = selectionKey;
        this.channel = (SocketChannel) selectionKey.channel();
        this.socket = this.channel.socket();
        this.executor = connector.getExecutor();
    }

    public Poller getPoller() {
        return poller;
    }

    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public Connector getConnector() {
        return connector;
    }

    public Executor getExecutor() {
        return executor;
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public void setHttpMethod(HttpMethod method) {
        this.httpMethod = method;
    }

    @Override
    public void setHttpUri(HttpUri uri) {
        this.httpUri = uri;
    }

    @Override
    public void setHttpVersion(HttpVersion version) {
        this.httpVersion = version;
    }

    @Override
    public HttpField getHttpField(String name) {
        return this.httpFields.get(name);
    }

    public HttpField getHttpField(HttpHeader header) {
        return this.getHttpField(header.asString());
    }

    @Override
    public void addHttpHeader(HttpField field) {
        this.httpFields.put(field);
    }

    @Override
    public void addHttpTrailer(HttpField field) {
        this.httpFields.put(field);
    }

    @Override
    public void addContent(ByteBuffer buffer) {
        this.httpInput.writeBuffer(buffer);
    }

    @Override
    public void requestComplete() {
        this.httpInput.flip();
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public HttpUri getHttpUri() {
        return httpUri;
    }

    public HttpVersion getHttpVersion() {
        return httpVersion;
    }

    public HttpFields getHttpFields() {
        return httpFields;
    }

    public HttpInput getHttpInput() {
        return httpInput;
    }
}
