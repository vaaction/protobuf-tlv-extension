package me.ibukanov.format;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import com.google.protobuf.Message;

public abstract class ProtobufWriter {
    private Charset defaultCharset = Charset.defaultCharset();

	public void print(final Message message, OutputStream output) throws IOException {
		print(message, output, defaultCharset);
	}

	abstract public void print(final Message message, OutputStream output, Charset cs) throws IOException;

	public byte[] printToBytes(final Message message) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			print(message, out, defaultCharset);
			out.flush();
			return out.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("Writing to a StringBuilder threw an IOException (should never happen).",
					e);
		}
	}
}
