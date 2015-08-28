package me.ibukanov.format;

import java.math.BigInteger;
import java.util.regex.Pattern;


public class Utils {

    private static final Pattern DIGITS = 
            Pattern.compile("[0-9]", Pattern.CASE_INSENSITIVE);

    public static Integer unsignedInt(int value) {
        if (value < 0) {
            return (int) ((value) & 0x00000000FFFFFFFFL);
        }
        return value;
    }

    public static Long unsignedLong(long value) {
        if (value < 0) {
            return BigInteger.valueOf(value & 0x7FFFFFFFFFFFFFFFL).setBit(63).longValue();
        }
        return value;
    }
    
    public static boolean isDigits(final String text) {
        return DIGITS.matcher(text).matches();
    }
}
