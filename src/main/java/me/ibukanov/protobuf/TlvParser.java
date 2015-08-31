package me.ibukanov.protobuf;

import java.util.ArrayList;
import java.util.Collection;

class TlvParser {

    private final byte[] value;
    private final int index;
    private final int length;
    private final int tag;
    private final Collection<TlvParser> children = new ArrayList<TlvParser>();

    public TlvParser(byte[] value, int tag) {
        this(value, 0, value.length, tag);
        parse();
    }

    private TlvParser(byte[] value, int index, int length, int tag) {
        if (value == null)
            throw new IllegalArgumentException("value must not be null");

        this.value = value;
        this.index = index;
        this.length = length;
        this.tag = tag;
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

    private void parse() {
        int index = this.index;
        int endIndex = this.index + length;

        while (index < endIndex) {
            int tag = getNext(index++);
            int length = getNext(index++);
            TlvParser tlv = new TlvParser(value, index, length, tag);
            children.add(tlv);
            index += tlv.getLength();
        }
    }

    private int getLength() {
        return length;
    }

    private int getNext(int index) {
        return (value[index] & 0xFF);
    }
}
