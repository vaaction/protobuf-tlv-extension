package me.ibukanov.protobuf;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


public class ProtobufTlvFormatterTest {
    private ProtobufTlvFormatter formatter = new ProtobufTlvFormatter();

    @Test
    public void noChildrenTest() throws Exception {
        UnittestProto.TestFieldOrderings testFieldOrderings = UnittestProto.TestFieldOrderings.newBuilder()
                .setMyFloat(1.0f)
                .setMyInt(1)
                .setMyString("1")
                .build();
        byte[] bytes = formatter.toByteArray(testFieldOrderings);

        UnittestProto.TestFieldOrderings.Builder builder = UnittestProto.TestFieldOrderings.newBuilder();
        formatter.parseFrom(bytes, builder);
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
        byte[] bytes = formatter.toByteArray(testCamelCaseFieldNames);

        UnittestProto.TestCamelCaseFieldNames.Builder builder = UnittestProto.TestCamelCaseFieldNames.newBuilder();
        formatter.parseFrom(bytes, builder);
        assertEquals(testCamelCaseFieldNames, builder.build());
    }
}
