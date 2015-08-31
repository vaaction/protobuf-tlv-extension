package me.ibukanov.protobuf;


import com.google.protobuf.Message;

public interface ProtobufFormatter {

    byte[] toByteArray(Message message);
    void parseFrom(byte[] input, Message.Builder builder);
}