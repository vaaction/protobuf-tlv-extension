package me.ibukanov.protobuf;


import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

public class ProtobufProtoFormatter implements ProtobufFormatter {

    public byte[] toByteArray(Message message) {
        return message.toByteArray();
    }

    public void parseFrom(byte[] input, Message.Builder builder) {
        try {
            builder.mergeFrom(input);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

}