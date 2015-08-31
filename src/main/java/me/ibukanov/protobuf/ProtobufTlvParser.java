package me.ibukanov.protobuf;

import com.google.protobuf.*;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ProtobufTlvParser {

    private static final int TOP_LEVEL_TAG = 0xFFFF;

    public void parseFrom(byte[] input, Message.Builder builder) {
        try {
            mergeFields(input, builder, TOP_LEVEL_TAG);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void mergeFields(byte[] input, Message.Builder builder, int tag) throws IOException {
        TlvParser tlvParser = new TlvParser(input, tag);
        for (TlvParser child: tlvParser.getChildren()) {
            mergeField(child, builder);
        }
    }

    private void mergeField(TlvParser tlv, Message.Builder builder) throws IOException {
        Descriptors.Descriptor type = builder.getDescriptorForType();
        if (tlv.getTag() != 0) {
            Descriptors.FieldDescriptor field = type.findFieldByNumber(tlv.getTag());
            handleValue(tlv, builder, field);
        }
    }

    private void handleValue(TlvParser tlv, Message.Builder builder, Descriptors.FieldDescriptor field) throws IOException {

        Object value;
        if (field.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
            value = handleObject(tlv, builder, field);
        } else {
            value = handlePrimitive(tlv, field);
        }
        if (value != null) {
            if (field.isRepeated()) {
                builder.addRepeatedField(field, value);
            } else {
                builder.setField(field, value);
            }
        }
    }

    private Object handlePrimitive(TlvParser tlv, Descriptors.FieldDescriptor field) throws IOException {
        int token = tlv.getTag();
        ByteBuffer value = ByteBuffer.wrap(tlv.getValue());

        switch (field.getType()) {
            case INT32:
            case SINT32:
            case SFIXED32:
                return value.getInt();

            case INT64:
            case SINT64:
            case SFIXED64:
                return value.getLong();

            case UINT32:
            case FIXED32:
                int valueInt = value.getInt();
                if (valueInt < 0) {
                    throw new NumberFormatException("Number must be positive: " + valueInt);
                }
                return valueInt;

            case UINT64:
            case FIXED64:
                long valueLong = value.getLong();
                if (valueLong < 0) {
                    throw new NumberFormatException("Number must be positive: " + valueLong);
                }
                return valueLong;

            case FLOAT:
                return value.getFloat();

            case DOUBLE:
                return value.getDouble();

            case BOOL:
                return value.getInt() > 0;

            case STRING:
                return new String(value.array());

            case BYTES:
                return value.array();

            case ENUM: {
                Descriptors.EnumDescriptor enumType = field.getEnumType();
                Descriptors.EnumValueDescriptor enumValue = enumType.findValueByName(String.valueOf(token));
                if (enumValue == null) {
                    throw new RuntimeException("Enum type \""
                            + enumType.getFullName()
                            + "\" has no value named \""
                            + token + "\".");
                }
                return enumValue;
            }
            case MESSAGE:
            case GROUP:
                throw new RuntimeException("Can't get here.");
            default:
                throw new RuntimeException("Unknown type");
        }
    }


    private Object handleObject(TlvParser tlv, Message.Builder builder,
                                Descriptors.FieldDescriptor field) throws IOException {
        Message.Builder subBuilder = builder.newBuilderForField(field);
        mergeFields(tlv.getValue(), subBuilder, tlv.getTag());
        return subBuilder.build();
    }
}
