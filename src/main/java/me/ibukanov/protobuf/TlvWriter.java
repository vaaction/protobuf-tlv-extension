package me.ibukanov.protobuf;

import java.io.IOException;
import java.io.OutputStream;

class TlvWriter {
    private final OutputStream outputStream;

    TlvWriter(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void write(int tag, byte[] value) throws IOException {
        writeTag(tag);
        int length = value.length;
        writeLength(length);
        writeData(value);
    }

    public void writeTag(int tag) throws IOException {
        outputStream.write(tag);
    }

    public void writeLength(int v) throws IOException {
        outputStream.write(v);
    }

    public void writeData(byte[] b) throws IOException {
        outputStream.write(b);
    }
}
