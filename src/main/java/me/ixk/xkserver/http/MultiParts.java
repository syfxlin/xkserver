/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.Part;
import me.ixk.xkserver.utils.MultiMap;
import me.ixk.xkserver.utils.ResourceUtils;

/**
 * @author Otstar Lin
 * @date 2020/11/10 下午 1:30
 */
public class MultiParts {
    private final MultiMap<Part> parts = new MultiMap<>();
    private final MultiPartConfig multipartConfig;
    private final File tmpDir;
    private final String contentType;

    public MultiParts(
        final MultiPartConfig multipartConfig,
        final String contentType
    ) {
        this.multipartConfig = multipartConfig;
        this.contentType = contentType;
        if (
            contentType == null ||
            !contentType.startsWith(MimeType.MULTIPART_FORM_DATA.asString())
        ) {
            throw new IllegalArgumentException(
                "Content-Type is not multipart/form-data"
            );
        }
        try {
            this.tmpDir =
                ResourceUtils.getFile(
                    multipartConfig.getLocation() == null
                        ? System.getProperty("java.io.tmpdir")
                        : multipartConfig.getLocation()
                );
        } catch (final FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public String getContentType() {
        return contentType;
    }

    public MultiMap<Part> getParts() {
        return parts;
    }

    public MultiPartConfig getMultiPartConfig() {
        return multipartConfig;
    }

    public File getTmpDir() {
        return tmpDir;
    }

    public void addPart(final Part part) {
        this.parts.add(part.getName(), part);
    }

    public class MultiPart implements Part {
        private String name;
        private String fileName;
        private HttpFields headers;
        private File file;
        // 用于写入的 OutputStream，当未超过 FileSizeThreshold 时是 fileOut（BufferedOutputStream），超过时时 memoryOut（ByteArrayOutputStream）
        private OutputStream out;
        private long size;

        public MultiPart() {
            this.out = new ByteArrayOutputStream();
        }

        public MultiPart(
            final String name,
            final String fileName,
            final HttpFields headers
        ) {
            this.name = name;
            this.fileName = fileName;
            this.headers = headers;
            this.out = new ByteArrayOutputStream();
        }

        public void readName() {
            final HttpField cd =
                this.headers.get(HttpHeader.CONTENT_DISPOSITION.asString());
            final String name = cd.getParam("name");
            final String filename = cd.getParam("filename");
            this.setName(
                    name == null ? null : name.substring(1, name.length() - 1)
                );
            this.setFileName(
                    fileName == null
                        ? null
                        : filename.substring(1, filename.length() - 1)
                );
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public HttpFields getHeaders() {
            return headers;
        }

        public void setHeaders(HttpFields headers) {
            this.headers = headers;
        }

        public void write(final ByteBuffer buffer) throws IOException {
            this.write(
                    buffer.array(),
                    buffer.arrayOffset() + buffer.position(),
                    buffer.remaining()
                );
        }

        public void write(final byte[] bytes, int offset, int length)
            throws IOException {
            this.check(length);
            this.out.write(bytes, offset, length);
            this.size += length;
        }

        public void write(final int b) throws IOException {
            this.check(1);
            this.out.write(b);
            this.size++;
        }

        public void flushWrite() throws IOException {
            this.out.flush();
        }

        private void check(int length) throws IOException {
            final long maxFileSize = multipartConfig.getMaxFileSize();
            if (maxFileSize > 0 && this.size + length > maxFileSize) {
                throw new IllegalStateException(
                    "Multipart " + name + " exceeds max filesize"
                );
            }
            final int fileSizeThreshold = multipartConfig.getFileSizeThreshold();
            if (
                file == null &&
                fileSizeThreshold > 0 &&
                this.size + length > fileSizeThreshold
            ) {
                this.createFile();
            }
        }

        private void createFile() throws IOException {
            this.file = FileUtil.createTempFile(tmpDir);
            final BufferedOutputStream fileOut = new BufferedOutputStream(
                FileUtil.getOutputStream(file)
            );
            // 将内存缓冲区的数据传入文件中，并同时销毁内存缓冲区
            final ByteArrayOutputStream memoryOut = (ByteArrayOutputStream) this.out;
            memoryOut.writeTo(fileOut);
            memoryOut.close();
            // 将数据流向调整为 File
            this.out = fileOut;
        }

        @Override
        public String toString() {
            return String.format(
                "Part{n=%s,fn=%s,s=%d,file=%s}",
                name,
                fileName,
                size,
                file
            );
        }

        @Override
        public InputStream getInputStream() throws IOException {
            // 已经写入到文件
            if (this.file != null) {
                return IoUtil.toStream(this.file);
            } else {
                return new ByteArrayInputStream(
                    ((ByteArrayOutputStream) out).toByteArray()
                );
            }
        }

        @Override
        public String getContentType() {
            final String contentType =
                this.getHeader(HttpHeader.CONTENT_TYPE.asString());
            return contentType == null
                ? MultiParts.this.contentType
                : contentType;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getSubmittedFileName() {
            return this.fileName;
        }

        @Override
        public long getSize() {
            return this.size;
        }

        @Override
        public void write(final String path) throws IOException {
            final File file;
            if (ResourceUtils.isUrl(path) || FileUtil.isAbsolutePath(path)) {
                file = ResourceUtils.getFile(path);
            } else {
                file = FileUtil.file(tmpDir, path);
            }
            out.flush();
            if (this.file != null) {
                FileUtil.move(this.file, file, true);
            } else {
                final ByteArrayOutputStream out = (ByteArrayOutputStream) this.out;
                try (
                    final BufferedOutputStream fileOut = FileUtil.getOutputStream(
                        file
                    )
                ) {
                    out.writeTo(fileOut);
                    fileOut.flush();
                }
            }
        }

        @Override
        public void delete() throws IOException {
            if (this.file != null && this.file.exists()) {
                if (!this.file.delete()) {
                    throw new IOException("Could not delete file");
                }
            }
        }

        @Override
        public String getHeader(final String name) {
            return this.headers.getValue(name);
        }

        @Override
        public Collection<String> getHeaders(final String name) {
            return this.headers.getValues(name);
        }

        @Override
        public Collection<String> getHeaderNames() {
            return this.headers.keySet();
        }
    }

    public static class MultiPartConfig {
        private volatile String location;
        private final long maxFileSize;
        private final long maxRequestSize;
        private final int fileSizeThreshold;

        public MultiPartConfig(
            final String location,
            final long maxFileSize,
            final long maxRequestSize,
            final int fileSizeThreshold
        ) {
            this.location = location;
            this.maxFileSize = maxFileSize;
            this.maxRequestSize = maxRequestSize;
            this.fileSizeThreshold = fileSizeThreshold;
        }

        public MultiPartConfig(final MultipartConfig annotation) {
            this.location =
                annotation.location().isEmpty() ? null : annotation.location();
            this.maxFileSize = annotation.maxFileSize();
            this.maxRequestSize = annotation.maxRequestSize();
            this.fileSizeThreshold = annotation.fileSizeThreshold();
        }

        public void setLocation(final String location) {
            this.location = location;
        }

        public String getLocation() {
            return location;
        }

        public long getMaxFileSize() {
            return maxFileSize;
        }

        public long getMaxRequestSize() {
            return maxRequestSize;
        }

        public int getFileSizeThreshold() {
            return fileSizeThreshold;
        }
    }
}
