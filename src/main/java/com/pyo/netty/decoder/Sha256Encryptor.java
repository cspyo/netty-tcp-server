package com.pyo.netty.decoder;

import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

@Component
public class Sha256Encryptor {

    // 모든 2bytes 값들을 SHA256 으로 해싱한 후,
    // <해싱 후 앞 3자리, 해싱 전 값> 을 매핑해서 저장하는 함수.
    // 미리 레인보우 테이블을 생성하여 ENCOUNTER 요청 시 빠르게 접근 가능.
    public HashMap<String, String> makeRainbowTable() {
        HashMap<String, String> rainbowTable = new HashMap<>();

        for (int i = 0; i <= 0xFFFF; i++) {
            short value = (short) i;
            byte[] bytes = new byte[]{(byte) (value >>> 8), (byte) value};
            String hashed;
            try {
                hashed = hash256(bytes);
            } catch (NoSuchAlgorithmException exception) {
                throw new RuntimeException("Could not get SHA-256 algorithm", exception);
            }

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("0");
            stringBuilder.append(hashed.substring(0, 3));
            String stringForKey = stringBuilder.toString();

            String stringForValue = bytesToString(bytes);

            if (!rainbowTable.containsKey(stringForKey)) {
                rainbowTable.put(stringForKey.toString(), stringForValue);
            }
        }
        return rainbowTable;
    }

    // SHA256 알고리즘으로 해싱하는 함수(hex)
    // byte 배열을 받고 해싱한 값을 String 으로 반환
    public static String hash256(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Could not get SHA-256 algorithm", ex);
        }
        md.update(bytes);
        return bytesToString(md.digest());
    }

    // byte 배열을 그대로 문자열로 반환하는 함수.
    // {0x05, 0x05} => "0505"
    public static String bytesToString(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte byt : bytes)
            result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }

    // 문자열을 그대로 크기 2의 byte 배열로 변환하는 함수.
    // "04a3" => {0x04, 0xa3}
    public static byte[] stringToBytes(String hexString) {
        int value = Integer.parseInt(hexString, 16);
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.putShort((short)value);
        byte[] bytes = buffer.array();
        return bytes;
    }
}
