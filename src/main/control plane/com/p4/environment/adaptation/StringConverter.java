/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.p4.environment.adaptation;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import jakarta.xml.bind.DatatypeConverter;
import java.util.Random;
import java.util.zip.CRC32;
import org.apache.commons.lang3.RandomStringUtils;

/**
 *
 * @author user
 */
public class StringConverter {

    /**
     * We assume here the url is valid, which means that it has at most 8 name
     * components, and each name component has at most 31 characters. This
     * method will generate a scapy friendly packet as a sequence of bytes (raw
     * bytes). The structure of each packet will be:
     * <1byte for #comps><triple 1><triple 2>...<triple n>
     * where
     * <triple> = <table_id><component hash><Fx>
     * For example: packet /a/b/c will produce [3 <1 h(a) 0> <1 h(b) h(/a)>
     * <1 h(c) h(/a/b)>]
     */
    public static byte[] convertUrlIntoScapyFriendlyPacket(String valid_url, boolean add_payload) {

        //we first set the number of name components
        String p[] = valid_url.split("/");
        byte number_components = (byte) (p.length - 1);

        String payload = "";
        byte packet[] = null;

        if (add_payload) {
            Random r = new Random();
            int x = r.nextInt(100, 200);
            payload = RandomStringUtils.random(x, true, true);
            packet = new byte[1 + number_components * 9 + payload.getBytes().length];
        } else {
            packet = new byte[1 + number_components * 9];
        }

        //We allocate space for the packet. 1 byte for the number of components and space for the n triples
        packet[0] = number_components;
        int index = 1;

        CRC32 crc = new CRC32();
        String subprefix = "";

        //filling the triple fields
        for (int i = 1; i < p.length; i++) {
            packet[index] = (byte) p[i].length();
            index++;

            crc.update(p[i].getBytes());
            byte[] component_hash_bytes = convertIntToByteArray((int) crc.getValue());
            System.arraycopy(component_hash_bytes, 0, packet, index, component_hash_bytes.length);
            index = index + component_hash_bytes.length;

            byte[] Fx = convertIntToByteArray(0);
            subprefix = subprefix + "/" + p[i];
            if (i == 1) {
                System.arraycopy(Fx, 0, packet, index, Fx.length);
                index = index + Fx.length;
            } else {
                crc.reset();
                crc.update(subprefix.getBytes());
                Fx = convertIntToByteArray((int) crc.getValue());
                System.arraycopy(Fx, 0, packet, index, Fx.length);
                index = index + Fx.length;
            }
            crc.reset();
        }

        if (add_payload) {
            System.arraycopy(payload.getBytes(), 0, packet, index, payload.getBytes().length);
        }

        return packet;
    }

    /**
     * We assume here the url is valid, which means that it has at most 8 name
     * components, and each name component has at most 31 characters. This
     * method will generate a scapy friendly packet as a sequence of bytes (raw
     * bytes). The structure of each packet will be:
     * <2bytes for packet id><1byte for #comps><size comp1><size comp2>...<size compn><characters without slashes>
     * For example: packet /a/b/c with id = 1 will produce [0, 1, 3, 1, 1, 1,
     * 97, 98, 99], where 97= 'a' converted into integer, 98='b' converted into
     * integer, and 99='c' converted into integer
     */
    public static byte[] convertUrlIntoScapyFriendlyPacketOldVersion(short id, String valid_url) {

        //First we take the id and convert it to a byte array of size 2 (id_bytes)
        ByteBuffer b = ByteBuffer.allocate(2);
        b.putShort(id);
        byte id_bytes[] = b.array();

        //Then we set the number of name components
        String p[] = valid_url.split("/");
        byte number_components = (byte) (p.length - 1);

        //We allocate space for the following fields: packet_id=2bytes, number_comps=1byte, components=(p.length - 1)bytes
        byte packet_without_name[] = new byte[2 + 1 + number_components];

        //filling the packet id (first two octets)
        System.arraycopy(id_bytes, 0, packet_without_name, 0, id_bytes.length);

        //filling the number of name components in the packet (third octet)
        packet_without_name[2] = number_components;

        //filling the component sizes in the packet..
        for (int i = 1; i < p.length; i++) {
            packet_without_name[i + 2] = (byte) p[i].length();
        }

        //now we remove all slashes from the url to store it into the packet.
        String url_without_slash = valid_url.replaceAll("/", "");
        byte[] name = url_without_slash.getBytes(StandardCharsets.US_ASCII);

        //we create a final packet (packet without name byte array + name byte array)
        byte[] packet = new byte[packet_without_name.length + name.length];
        System.arraycopy(packet_without_name, 0, packet, 0, packet_without_name.length);
        System.arraycopy(name, 0, packet, packet_without_name.length, name.length);

        return packet;
    }

    /**
     * This method takes a name and convert it into hexadecimal. For example:
     * string 'abc' is converted to 0x616263
     */
    public static String convertNameToHex(String ascii) {

        // Convert ASCII string to char array
        char[] ch = ascii.toCharArray();

        // Iterate over char array and cast each element to Integer.
        StringBuilder builder = new StringBuilder();
        for (char c : ch) {
            int i = (int) c;
            // Convert integer value to hex using toHexString() method.
            builder.append(Integer.toHexString(i));
        }

        // Return the string in hexadecimal..
        return "0x" + builder.toString();
    }

    /**
     * This method takes a BMv2 generated string that represents a given
     * timestamp (whether a receive or a send timestamp), convert it into number
     * and returns it
     */
    public static long convertTimestampToLong(String timestamp) {
        return 0;
    }

    public static String convertIntegerToBitstring(int x, int digits) {
        String s = "";
        if (digits <= 32) {
            s = Integer.toBinaryString(x);
            while (s.length() < digits) {
                s = "0" + s;
            }
        }
        return s;
    }

    public static String convertIntegerToBitstring(int x) {
        String s = Integer.toBinaryString(x);
        while (s.length() < 32) {
            s = "0" + s;
        }
        return s;
    }

    public static String convertLongToBitstring(long x) {
        String s = Long.toBinaryString(x);
        while (s.length() < 64) {
            s = "0" + s;
        }
        return s;
    }

    public static String convertByteToBitstring(byte x) {

        String s = Integer.toBinaryString(Byte.toUnsignedInt(x));
        if (s.length() > 8) {
            int start_index = s.length() - 8;
            int end_index = s.length();
            s = s.substring(start_index, end_index);
        } else {
            while (s.length() < 8) {
                s = "0" + s;
            }
        }
        return s;
    }

    public static short convertHexStringToShort(String hexString) {
        byte id_bytes[] = DatatypeConverter.parseHexBinary(hexString);
        BigInteger b = new BigInteger(id_bytes);
        return b.shortValueExact();
    }

    public static final byte[] convertIntToByteArray(int value) {
        return new byte[]{
            (byte) (value >>> 24),
            (byte) (value >>> 16),
            (byte) (value >>> 8),
            (byte) value};
    }
}
