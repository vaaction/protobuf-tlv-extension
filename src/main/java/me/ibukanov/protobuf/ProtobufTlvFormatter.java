package me.ibukanov.protobuf;


import com.google.protobuf.Message;

public class ProtobufTlvFormatter {

    private final ProtobufTlvWriter writer = new ProtobufTlvWriter();
    private final ProtobufTlvParser parser = new ProtobufTlvParser();

    public byte[] toByteArray(Message message) {
        return writer.toByteArray(message);
    }

    public void parseFrom(byte[] input, Message.Builder builder) {
        parser.parseFrom(input, builder);
    }

}