package me.ibukanov.format;

import com.google.protobuf.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;

import static me.ibukanov.format.Utils.unsignedInt;
import static me.ibukanov.format.Utils.unsignedLong;


public final class TlvFormat extends ProtobufWriter {
    @Override
    public void print(Message message, OutputStream output, Charset cs) throws IOException {
        print(message, output);
    }

    public void print(Message message, OutputStream output) throws IOException {
        printMessage(message, new TLVOutputStream(output));
    }

    protected void printMessage(Message message, TLVOutputStream output) throws IOException {

        for (Iterator<Map.Entry<Descriptors.FieldDescriptor, Object>> iter = message.getAllFields().entrySet().iterator(); iter.hasNext();) {
            Map.Entry<Descriptors.FieldDescriptor, Object> field = iter.next();
            printField(field.getKey(), field.getValue(), output);
        }
    }

    public void printField(Descriptors.FieldDescriptor field, Object value, TLVOutputStream output) throws IOException {

        printSingleField(field, value, output);
    }

    private void printSingleField(Descriptors.FieldDescriptor field,
                                  Object value,
                                  TLVOutputStream output) throws IOException {
        if (field.isRepeated()) {
            for (Iterator<?> iter = ((List<?>) value).iterator(); iter.hasNext();) {
                printFieldValue(field, iter.next(), output);
            }
        } else {
            printFieldValue(field, value, output);
        }
    }

    private void printFieldValue(Descriptors.FieldDescriptor field, Object value, TLVOutputStream output) throws IOException {
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
                bytes = new byte[]{(byte)((Boolean) value ? 0x01 : 0x00)};
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
                output.write(field.getNumber(), ((Descriptors.EnumValueDescriptor) value).getName().getBytes());
                return;
            }

            case MESSAGE:
            case GROUP:
                printMessage((Message) value, output);
        }
    }

    public void merge(byte[] input,
                      final Message.Builder builder) throws IOException {
        final ExtensionRegistry extensionRegistry = ExtensionRegistry.getEmptyRegistry();
        try {
            Tlv tlv = new Tlv(input);
            if (tlv.isConstructed()) {
                for (Tlv child: tlv.getChildren()) {
                    mergeField(child, extensionRegistry, builder);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void mergeField(Tlv tlv,
                              ExtensionRegistry extensionRegistry,
                              Message.Builder builder) throws IOException {
        Descriptors.FieldDescriptor field = null;
        Descriptors.Descriptor type = builder.getDescriptorForType();
        boolean unknown = false;
        ExtensionRegistry.ExtensionInfo extension = null;

        if (tlv.getTag() != 0) {
            field = type.findFieldByNumber(tlv.getTag());

            // Group names are expected to be capitalized as they appear in the
            // .proto file, which actually matches their type names, not their field
            // names.
            if (field == null) {
                // Explicitly specify US locale so that this code does not break when
                // executing in Turkey.
                String lowerName = field.getFullName().toLowerCase(Locale.US);
                field = type.findFieldByName(lowerName);
                // If the case-insensitive match worked but the field is NOT a group,
                if ((field != null) && (field.getType() != Descriptors.FieldDescriptor.Type.GROUP)) {
                    field = null;
                }
            }
            // Again, special-case group names as described above.
            if ((field != null) && (field.getType() == Descriptors.FieldDescriptor.Type.GROUP)
                    && !field.getMessageType().getName().equals(field.getFullName())
                    && !field.getMessageType().getFullName().equalsIgnoreCase(field.getFullName()) /* extension */) {
                field = null;
            }

            // Last try to lookup by field-index if 'name' is numeric,
            // which indicates a possible unknown field
            if (field == null && Utils.isDigits(field.getFullName())) {
                field = type.findFieldByNumber(tlv.getTag());
                unknown = true;
            }
        }

        if (field != null) {
            if (!tlv.getChildren().isEmpty()) {
                for (Tlv childTlv: tlv.getChildren()) {
                    handleValue(childTlv, extensionRegistry, builder, field, extension, unknown);
                }
            } else {
                handleValue(tlv, extensionRegistry, builder, field, extension, unknown);
            }
        }
    }

    private void handleValue(Tlv tlv,
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

    private Object handlePrimitive(Tlv tlv, Descriptors.FieldDescriptor field) throws IOException {
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


    private Object handleObject(Tlv tlv,
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
        for (Tlv childTlv : tlv.getChildren()) {
            mergeField(childTlv, extensionRegistry, subBuilder);
        }
        return subBuilder.build();
    }



    class Tlv {
        public class TlvException extends Exception {
            private static final long serialVersionUID = -2427261641980591073L;
        }

        private static final int TAG_TOPLEVEL = 0xFFFF;

        private byte[] mValue;

        private int mIndex;

        private int mLength;

        private int mTag;

        private List<Tlv> mChildren;

        public Tlv(byte[] value) throws TlvException {
            this(value, 0, value.length, TAG_TOPLEVEL);
        }

        private Tlv(byte[] value, int index, int length, int tag) throws TlvException {
            if (value == null)
                throw new IllegalArgumentException("value must not be null");

            mValue = value;
            mIndex = index;
            mLength = length;
            mTag = tag;
            mChildren = new LinkedList<Tlv>();

            if (isConstructed())
            {
                parse();
            }
        }

        public int getTag()
        {
            return mTag;
        }

        public byte[] getValue()
        {
            byte[] newArray = new byte[mLength];
            System.arraycopy(mValue, mIndex, newArray, 0, mLength);
            return newArray;
        }

        public List<Tlv> getChildren()
        {
            return mChildren;
        }

        public boolean isConstructed()
        {
            final int CONSTRUCTED_BIT = 0x20;
            return (getFirstTagByte(mTag) & CONSTRUCTED_BIT) != 0;
        }

        private void parse() throws TlvException
        {
            int index = mIndex;
            int endIndex = mIndex + mLength;

            while (index < endIndex)
            {
                int tag = getNext(index++);

                if (tag == 0x00 || tag == 0xFF)
                    continue;

                if (tagHasMultipleBytes(tag))
                {
                    tag <<= 8;
                    tag |= getNext(index++);

                    if (tagHasAnotherByte(tag))
                    {
                        tag <<= 8;
                        tag |= getNext(index++);
                    }

                    if (tagHasAnotherByte(tag))
                        throw new TlvException();
                }

                int length = getNext(index++);

                if (length >= 0x80)
                {
                    int numLengthBytes = (length & 0x7F);

                    if (numLengthBytes > 3)
                        throw new TlvException();

                    length = 0;

                    for (int i = 0; i < numLengthBytes; i++)
                    {
                        length <<= 8;
                        length |= getNext(index++);
                    }
                }

                Tlv tlv = new Tlv(mValue, index, length, tag);
                mChildren.add(tlv);
                index += tlv.getLength();
            }
        }

        private int getLength()
        {
            return mLength;
        }

        private int getNext(int index) throws TlvException
        {
            if (index < mIndex || index >= mIndex + mLength)
                throw new TlvException();

            return (mValue[index] & 0xFF);
        }

        private int getFirstTagByte(int tag)
        {
            while (tag > 0xFF)
                tag >>= 8;

            return tag;
        }

        private boolean tagHasMultipleBytes(int tag)
        {
            final int MULTIBYTE_TAG_MASK = 0x1F;
            return (tag & MULTIBYTE_TAG_MASK) == MULTIBYTE_TAG_MASK;
        }

        private boolean tagHasAnotherByte(int tag)
        {
            final int NEXT_BYTE = 0x80;
            return (tag & NEXT_BYTE) != 0;
        }
    }

    public class TLVOutputStream {
        private OutputStream outputStream;
        TLVOutputStream(OutputStream outputStream) {
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

}