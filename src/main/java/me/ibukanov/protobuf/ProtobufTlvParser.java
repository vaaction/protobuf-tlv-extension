package me.ibukanov.protobuf;

import com.google.protobuf.*;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ProtobufTlvParser {

    public void parseFrom(byte[] input, Message.Builder builder) {
        ExtensionRegistry extensionRegistry = ExtensionRegistry.getEmptyRegistry();
        try {
            TlvParser tlvParser = new TlvParser(input);
            if (tlvParser.isConstructed()) {
                for (TlvParser child: tlvParser.getChildren()) {
                    mergeField(child, extensionRegistry, builder);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void mergeField(TlvParser tlv,
                              ExtensionRegistry extensionRegistry,
                              Message.Builder builder) throws IOException {
        Descriptors.FieldDescriptor field = null;
        Descriptors.Descriptor type = builder.getDescriptorForType();
        boolean unknown = false;

        if (tlv.getTag() != 0) {
            field = type.findFieldByNumber(tlv.getTag());
            if ((field != null) && (field.getType() == Descriptors.FieldDescriptor.Type.GROUP)
                    && !field.getMessageType().getName().equals(field.getFullName())
                    && !field.getMessageType().getFullName().equalsIgnoreCase(field.getFullName())) {
                field = null;
            }
        }

        if (field != null) {
            if (!tlv.getChildren().isEmpty()) {
                for (TlvParser childTlv: tlv.getChildren()) {
                    handleValue(childTlv, extensionRegistry, builder, field, null, unknown);
                }
            } else {
                handleValue(tlv, extensionRegistry, builder, field, null, unknown);
            }
        }
    }

    private void handleValue(TlvParser tlv,
                             ExtensionRegistry extensionRegistry,
                             Message.Builder builder,
                             Descriptors.FieldDescriptor field,
                             ExtensionRegistry.ExtensionInfo extension,
                             boolean unknown) throws IOException {

        Object value;
        if (field.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
            value = handleObject(tlv, extensionRegistry, builder, field, extension, unknown);
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


    private Object handleObject(TlvParser tlv,
                                ExtensionRegistry extensionRegistry,
                                Message.Builder builder,
                                Descriptors.FieldDescriptor field,
                                ExtensionRegistry.ExtensionInfo extension,
                                boolean unknown) throws IOException {

        Message.Builder subBuilder;
        if (extension == null) {
            subBuilder = builder.newBuilderForField(field);
        } else {
            subBuilder = extension.defaultInstance.newBuilderForType();
        }

        if (unknown) {
            ByteString data = ByteString.copyFrom(tlv.getValue());
            try {
                subBuilder.mergeFrom(data);
                return subBuilder.build();
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException("Failed to build " + field.getFullName() + " from " + data);
            }
        }
        for (TlvParser childTlv : tlv.getChildren()) {
            mergeField(childTlv, extensionRegistry, subBuilder);
        }
        return subBuilder.build();
    }
}
