package me.ibukanov.format;

import org.junit.Ignore;
import org.junit.Test;
import protobuf_unittest.UnittestProto;

import static org.junit.Assert.*;


public class TlvFormatTest {
    private TlvFormat formatter = new TlvFormat();

    @Test
    public void noChildrenTest() throws Exception {
        UnittestProto.TestFieldOrderings testFieldOrderings = UnittestProto.TestFieldOrderings.newBuilder()
                .setMyFloat(1.0f)
                .setMyInt(1)
                .setMyString("1")
                .build();
        byte[] bytes = formatter.printToBytes(testFieldOrderings);

        UnittestProto.TestFieldOrderings.Builder builder = UnittestProto.TestFieldOrderings.newBuilder();
        formatter.merge(bytes, builder);
        assertEquals(testFieldOrderings, builder.build());
    }

    @Test
    @Ignore
    public void withChildrenTest() throws Exception {
        UnittestProto.TestCamelCaseFieldNames testCamelCaseFieldNames = UnittestProto.TestCamelCaseFieldNames.newBuilder()
                .setCordField("1")
                .setPrimitiveField(1)
                .setMessageField(UnittestProto.ForeignMessage.newBuilder().setC(2))
                .build();
        byte[] bytes = formatter.printToBytes(testCamelCaseFieldNames);

        UnittestProto.TestCamelCaseFieldNames.Builder builder = UnittestProto.TestCamelCaseFieldNames.newBuilder();
        formatter.merge(bytes, builder);
        assertEquals(testCamelCaseFieldNames, builder.build());
    }
}
