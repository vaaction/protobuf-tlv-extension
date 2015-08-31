package me.ibukanov.protobuf;

import java.util.ArrayList;
import java.util.Collection;

class TlvParser {

    private static final int TOP_LEVEL_TAG = 0xFFFF;
    private static final int NEXT_BYTE = 0x80;
    private static final int MULTI_BYTE_MASK_TAG = 0x1F;
    private static final int CONSTRUCTED_BIT = 0x20;

    public class TlvParserException extends Exception {}

    private final byte[] value;
    private final int index;
    private final int length;
    private final int tag;
    private final Collection<TlvParser> children;

    public TlvParser(byte[] value) throws TlvParserException {
        this(value, 0, value.length, TOP_LEVEL_TAG);
    }

    private TlvParser(byte[] value, int index, int length, int tag) throws TlvParserException {
        if (value == null)
            throw new IllegalArgumentException("value must not be null");

        this.value = value;
        this.index = index;
        this.length = length;
        this.tag = tag;
        children = new ArrayList<TlvParser>();

        if (isConstructed()) {
            parse();
        }
    }

    public int getTag() {
        return tag;
    }

    public byte[] getValue() {
        byte[] newArray = new byte[length];
        System.arraycopy(value, index, newArray, 0, length);
        return newArray;
    }

    public Collection<TlvParser> getChildren() {
        return children;
    }

    public boolean isConstructed() {
        return (getFirstTagByte(tag) & CONSTRUCTED_BIT) != 0;
    }

    private void parse() throws TlvParserException {
        int index = this.index;
        int endIndex = this.index + length;

        while (index < endIndex)
        {
            int tag = getNext(index++);

            if (tag == 0x00 || tag == 0xFF)
                continue;

            if (hasMultipleBytesTag(tag))
            {
                tag <<= 8;
                tag |= getNext(index++);

                if (hasAnotherByteTag(tag))
                {
                    tag <<= 8;
                    tag |= getNext(index++);
                }

                if (hasAnotherByteTag(tag))
                    throw new TlvParserException();
            }

            int length = getNext(index++);

            if (length >= NEXT_BYTE)
            {
                int numLengthBytes = (length & 0x7F);

                if (numLengthBytes > 3)
                    throw new TlvParserException();

                length = 0;

                for (int i = 0; i < numLengthBytes; i++)
                {
                    length <<= 8;
                    length |= getNext(index++);
                }
            }

            TlvParser tlv = new TlvParser(value, index, length, tag);
            children.add(tlv);
            index += tlv.getLength();
        }
    }

    private int getLength() {
        return length;
    }

    private int getNext(int index) throws TlvParserException {
        if (index < this.index || index >= this.index + length)
            throw new TlvParserException();

        return (value[index] & 0xFF);
    }

    private int getFirstTagByte(int tag) {
        while (tag > 0xFF)
            tag >>= 8;

        return tag;
    }

    private boolean hasMultipleBytesTag(int tag) {
        return (tag & MULTI_BYTE_MASK_TAG) == MULTI_BYTE_MASK_TAG;
    }

    private boolean hasAnotherByteTag(int tag) {
        return (tag & NEXT_BYTE) != 0;
    }
}
