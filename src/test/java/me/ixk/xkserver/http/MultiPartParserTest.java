/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.hutool.core.io.IoUtil;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map.Entry;
import javax.servlet.http.Part;
import me.ixk.xkserver.http.MultiPartParser.PartHandler;
import me.ixk.xkserver.http.MultiParts.MultiPart;
import me.ixk.xkserver.http.MultiParts.MultiPartConfig;
import org.junit.jupiter.api.Test;

/**
 * @author Otstar Lin
 * @date 2020/11/11 下午 1:38
 */
class MultiPartParserTest {

    @Test
    void parse() throws IOException {
        final PartHandlerImpl handler = new PartHandlerImpl();
        final MultiPartParser parser = new MultiPartParser(
            handler,
            "WebAppBoundary"
        );
        parser.parse(this.partContent());
        final MultiParts parts = handler.getMultiParts();
        this.assertParts(parts);
    }

    @Test
    void splitParse() throws IOException {
        final PartHandlerImpl handler = new PartHandlerImpl();
        final MultiPartParser parser = new MultiPartParser(
            handler,
            "WebAppBoundary"
        );
        for (byte b : this.partContent().array()) {
            parser.parse(ByteBuffer.wrap(new byte[] { b }));
        }
        final MultiParts parts = handler.getMultiParts();
        this.assertParts(parts);
    }

    private static class PartHandlerImpl implements PartHandler {
        private final MultiParts multiParts = new MultiParts(
            new MultiPartConfig(null, -1, -1, 0),
            MimeType.MULTIPART_FORM_DATA.asString()
        );
        private MultiPart part;

        public MultiParts getMultiParts() {
            return multiParts;
        }

        @Override
        public void startPart() {
            this.part = this.multiParts.new MultiPart();
            this.part.setHeaders(new HttpFields());
        }

        @Override
        public void addHttpHeader(final HttpField field) {
            this.part.getHeaders().put(field.getName(), field);
        }

        @Override
        public void addContent(final ByteBuffer buffer) {
            try {
                this.part.write(buffer);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void endPart() {
            this.part.readName();
            this.multiParts.addPart(this.part);
            this.part = null;
        }

        @Override
        public HttpField getHttpField(final String headerName) {
            return this.part.getHeaders().get(headerName);
        }
    }

    private ByteBuffer partContent() {
        return ByteBuffer.wrap(
            (
                "--WebAppBoundary\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"pom.xml\"\r\n" +
                "Content-Type: application/xml\r\n" +
                "Content-Transfer-Encoding: binary\r\n" +
                "\r\n" +
                "<properties>\n" +
                "  <project.java.version>11</project.java.version>\n" +
                "  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n" +
                "  <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>\n" +
                "</properties>" +
                "\r\n" +
                "--WebAppBoundary\r\n" +
                "Content-Disposition: form-data; name=\"element-name\"\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                "Name" +
                "\r\n" +
                "--WebAppBoundary--"
            ).getBytes()
        );
    }

    private void assertParts(StringBuilder builder) {
        assertEquals(
            "file: \n" +
            "Content-Type: application/xml\n" +
            "<properties>\n" +
            "  <project.java.version>11</project.java.version>\n" +
            "  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n" +
            "  <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>\n" +
            "</properties>\n" +
            "element-name: \n" +
            "Content-Type: text/plain\n" +
            "Name\n",
            builder.toString()
        );
    }

    private void assertParts(MultiParts parts) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (Entry<String, List<Part>> entry : parts.getParts().entrySet()) {
            builder.append(entry.getKey()).append(": \n");
            builder
                .append("Content-Type: ")
                .append(entry.getValue().get(0).getContentType())
                .append("\n");
            builder
                .append(IoUtil.read(entry.getValue().get(0).getInputStream()))
                .append("\n");
        }
        System.out.println(builder);
        this.assertParts(builder);
    }
}
