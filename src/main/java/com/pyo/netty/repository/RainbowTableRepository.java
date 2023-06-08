package com.pyo.netty.repository;

import com.pyo.netty.decoder.Sha256Encryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Component
public class RainbowTableRepository {
    // 모든 2bytes 값들을 SHA256 으로 해싱한 후,
    // {해싱 후 앞 3자리, 해싱 전 값} 을 매핑해서 저장하는 함수.
    // 미리 레인보우 테이블을 생성하여 ENCOUNTER 요청 시 빠르게 접근 가능.

    private final Sha256Encryptor sha256Encryptor;
    public static Map<String, String> rainbowTable = new HashMap<>();

    @Autowired
    public RainbowTableRepository(Sha256Encryptor sha256Encryptor) {
        this.sha256Encryptor = sha256Encryptor;
        rainbowTable = sha256Encryptor.makeRainbowTable();
    }

    public String findDecryptValue(String input) {
        return rainbowTable.get(input);
    }

}
