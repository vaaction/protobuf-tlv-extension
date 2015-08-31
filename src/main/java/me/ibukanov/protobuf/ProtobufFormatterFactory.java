package me.ibukanov.protobuf;

public class ProtobufFormatterFactory {

    public ProtobufFormatter createFormatter(String type) {
        if (type.equals("tlv")) {
            return new ProtobufTlvFormatter();
        } else {
            return new ProtobufProtoFormatter();
        }
    }

}