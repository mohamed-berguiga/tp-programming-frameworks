package net.thevpc.gaming.atom.examples.kombla.main.shared.dal;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

public class CountingOutputStream extends FilterOutputStream {
    private final AtomicLong counter;

    public CountingOutputStream(OutputStream out, AtomicLong counter) {
        super(out);
        this.counter = counter;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        counter.incrementAndGet();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        counter.addAndGet(len);
    }
}
