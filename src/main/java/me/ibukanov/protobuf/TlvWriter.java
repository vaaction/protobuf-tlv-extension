package me.ibukanov.protobuf;

import java.io.IOException;
import java.io.OutputStream;

class TlvWriter {
    private final OutputStream outputStream;

    TlvWriter(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void write(int tag, byte[] value) throws IOException {
        outputStream.write(tag);
        outputStream.write(value.length);
        outputStream.write(value);
    }
}
