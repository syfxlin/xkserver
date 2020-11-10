/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import me.ixk.xkserver.http.MultiParts.MultiPart;
import me.ixk.xkserver.http.MultiParts.MultiPartConfig;
import org.junit.jupiter.api.Test;

/**
 * @author Otstar Lin
 * @date 2020/11/10 下午 4:12
 */
class MultiPartsTest {

    @Test
    void bufferPart() throws IOException {
        MultiParts parts = new MultiParts(
            new MultiPartConfig(null, -1, -1, 0),
            MimeType.MULTIPART_FORM_DATA.asString()
        );
        final MultiPart part =
            parts.new MultiPart("file", "file.json", new HttpFields());
        final String fileString = "Test JSON file";
        for (byte ch : fileString.getBytes(StandardCharsets.UTF_8)) {
            part.write(ch);
        }
        assertEquals(
            fileString,
            IoUtil.read(part.getInputStream(), StandardCharsets.UTF_8)
        );
    }

    @Test
    void filePart() throws IOException {
        MultiParts parts = new MultiParts(
            new MultiPartConfig(null, -1, -1, 1),
            MimeType.MULTIPART_FORM_DATA.asString()
        );
        final MultiPart part =
            parts.new MultiPart("file", "file.json", new HttpFields());
        final String fileString = "Test JSON file";
        for (byte ch : fileString.getBytes(StandardCharsets.UTF_8)) {
            part.write(ch);
        }
        part.flushWrite();
        assertEquals(
            fileString,
            IoUtil.read(part.getInputStream(), StandardCharsets.UTF_8)
        );
    }

    @Test
    void writePart() throws IOException {
        MultiParts parts = new MultiParts(
            new MultiPartConfig(null, -1, -1, 0),
            MimeType.MULTIPART_FORM_DATA.asString()
        );
        final MultiPart part =
            parts.new MultiPart("file", "file.json", new HttpFields());
        final String fileString = "Test JSON file";
        for (byte ch : fileString.getBytes(StandardCharsets.UTF_8)) {
            part.write(ch);
        }
        final String tmpPath = "F:/tmp/123.txt";
        part.write(tmpPath);
        part.write('a');
        final String readString = FileUtil.readString(
            tmpPath,
            StandardCharsets.UTF_8
        );
        assertEquals(fileString, readString);
    }

    @Test
    void writePart2() throws IOException {
        MultiParts parts = new MultiParts(
            new MultiPartConfig(null, -1, -1, 1),
            MimeType.MULTIPART_FORM_DATA.asString()
        );
        final MultiPart part =
            parts.new MultiPart("file", "file.json", new HttpFields());
        final String fileString = "Test JSON file";
        for (byte ch : fileString.getBytes(StandardCharsets.UTF_8)) {
            part.write(ch);
        }
        final String tmpPath = "F:/tmp/123.txt";
        part.write(tmpPath);
        part.write('a');
        final String readString = FileUtil.readString(
            tmpPath,
            StandardCharsets.UTF_8
        );
        assertEquals(fileString, readString);
    }
}
