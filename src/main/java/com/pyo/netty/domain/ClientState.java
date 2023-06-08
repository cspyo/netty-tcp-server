package com.pyo.netty.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class ClientState {

    // Encounter 메세지를 보냈었는지 체크하는 필드
    private boolean encountered;

    // Hash 값을 클라이언트에 보내고 그에 대한 응답을 기록하는 필드
    private boolean check;

    // 클라이언트가 Encounter 로 보낸 메세지를 저장하기 위한 필드
    private String name;
}
