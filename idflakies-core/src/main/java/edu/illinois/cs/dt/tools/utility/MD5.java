package edu.illinois.cs.dt.tools.utility;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MD5 {
    // From: https://stackoverflow.com/a/6565597/1498618
    public static String md5(final String md5) {
        try {
            final byte[] array = MessageDigest.getInstance("md5").digest(md5.getBytes());

            final StringBuilder sb = new StringBuilder();

            for (final byte anArray : array) {
                sb.append(Integer.toHexString((anArray & 0xFF) | 0x100), 1, 3);
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException ignored) {}
        return "";
    }

    public static String hashOrder(final List<String> order) {
        return md5(String.join("", order));
    }

    @SafeVarargs
    public static String hashOrders(final List<String>... orders) {
        return hashOrder(Arrays.stream(orders).map(MD5::hashOrder).collect(Collectors.toList()));
    }
}
