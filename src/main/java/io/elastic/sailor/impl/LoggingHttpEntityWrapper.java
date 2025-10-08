package io.elastic.sailor.impl;

import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

public class LoggingHttpEntityWrapper extends HttpEntityWrapper {

    private static final Logger logger = LoggerFactory.getLogger(LoggingHttpEntityWrapper.class);

    public LoggingHttpEntityWrapper(HttpEntity wrappedEntity) {
        super(wrappedEntity);
    }

    @Override
    public void writeTo(OutputStream outstream) throws IOException {
        super.writeTo(new LoggingOutputStream(outstream));
    }

    private static class LoggingOutputStream extends OutputStream {
        private final OutputStream wrappedStream;
        private long bytesWritten = 0;
        private int chunkCount = 0;
        private static final int CHUNK_SIZE_TO_LOG = 1024 * 1024; // Log every 1MB

        public LoggingOutputStream(OutputStream wrappedStream) {
            this.wrappedStream = wrappedStream;
        }

        @Override
        public void write(int b) throws IOException {
            wrappedStream.write(b);
            bytesWritten++;
            logProgress();
        }

        @Override
        public void write(byte[] b) throws IOException {
            wrappedStream.write(b);
            bytesWritten += b.length;
            logProgress();
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            wrappedStream.write(b, off, len);
            bytesWritten += len;
            logProgress();
        }

        @Override
        public void flush() throws IOException {
            wrappedStream.flush();
        }

        @Override
        public void close() throws IOException {
            logger.debug("Finished writing entity. Total bytes: {}", bytesWritten);
            wrappedStream.close();
        }

        private void logProgress() {
            long newChunkCount = bytesWritten / CHUNK_SIZE_TO_LOG;
            if (newChunkCount > chunkCount) {
                chunkCount = (int) newChunkCount;
                logger.debug("Uploaded {} MB so far...", chunkCount);
            }
        }
    }
}
