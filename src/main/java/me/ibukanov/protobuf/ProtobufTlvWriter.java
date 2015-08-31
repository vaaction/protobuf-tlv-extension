package me.ibukanov.protobuf;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class ProtobufTlvWriter {

    public byte[] toByteArray(Message message) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            print(message, new TlvWriter(output));
            output.flush();
            return output.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void print(final Message message, TlvWriter output) throws IOException {
        for (Map.Entry<Descriptors.FieldDescriptor, Object> field : message.getAllFields().entrySet()) {
            printField(field.getKey(), field.getValue(), output);
        }
    }

    private void printMessage(Descriptors.FieldDescriptor field, Message message, TlvWriter output) throws IOException {
        byte[] serialized = toByteArray(message);
        output.write(field.getNumber(), serialized);
    }

    private void printField(Descriptors.FieldDescriptor field, Object value, TlvWriter output) throws IOException {
        printSingleField(field, value, output);
    }

    private void printSingleField(Descriptors.FieldDescriptor field,
                                  Object value,
                                  TlvWriter output) throws IOException {
        if (field.isRepeated()) {
            for (Object o : ((List<?>) value)) {
                printFieldValue(field, o, output);
            }
        } else {
            printFieldValue(field, value, output);
        }
    }

    private void printFieldValue(Descriptors.FieldDescriptor field, Object value, TlvWriter output) throws IOException {
        byte[] bytes;
        switch (field.getType()) {
            case INT32:
            case SINT32:
            case SFIXED32:
                bytes = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE).putInt((Integer)value).array();
                output.write(field.getNumber(), bytes);
                return;
            case INT64:
            case SINT64:
            case SFIXED64:
                bytes = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong((Long) value).array();
                output.write(field.getNumber(), bytes);
                return;

            case FLOAT:
                bytes = ByteBuffer.allocate(Float.SIZE / Byte.SIZE).putFloat((Float) value).array();
                output.write(field.getNumber(), bytes);
                return;

            case DOUBLE:
                bytes = ByteBuffer.allocate(Double.SIZE / Byte.SIZE).putDouble((Double) value).array();
                output.write(field.getNumber(), bytes);
                return;

            case BOOL:
                bytes = new byte[]{(byte)((Boolean) value ? 1 : 0)};
                output.write(field.getNumber(), bytes);
                return;

            case UINT32:
            case FIXED32:
                bytes = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE).putInt(unsignedInt((Integer) value)).array();
                output.write(field.getNumber(), bytes);
                return;

            case UINT64:
            case FIXED64:
                bytes = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(unsignedLong((Long) value)).array();
                output.write(field.getNumber(), bytes);
                return;

            case STRING:
                bytes = ((String) value).getBytes();
                output.write(field.getNumber(), bytes);
                return;

            case BYTES: {
                output.write(field.getNumber(), ((ByteString) value).toByteArray());
                return;
            }

            case ENUM: {
                bytes = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE)
                        .putInt(((Descriptors.EnumValueDescriptor) value).getIndex()).array();
                output.write(field.getNumber(), bytes);
                return;
            }

            case MESSAGE:
            case GROUP:
                printMessage(field, (Message) value, output);
        }
    }

    private static Integer unsignedInt(int value) {
        if (value < 0) {
            return (int) ((value) & 0x00000000FFFFFFFFL);
        }
        return value;
    }

    private static Long unsignedLong(long value) {
        if (value < 0) {
            return BigInteger.valueOf(value & 0x7FFFFFFFFFFFFFFFL).setBit(63).longValue();
        }
        return value;
    }

}
