package com.pyo.netty.repository;

import com.pyo.netty.domain.ClientState;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class ClientRepository {
    // DB 대신 In-Memory 로 클라이언트 정보를 저장

    // Netty 에서는 클라이언트를 ChannelHandlerContext 로 구분한다.
    // {key: ctx, value: 클라이언트의 상태} 를 HashMap 에 저장.
    private static Map<ChannelHandlerContext, ClientState> clientTable = new HashMap<>();

    public ClientState save(ChannelHandlerContext ctx, ClientState clientState) {
        clientTable.put(ctx, clientState);
        return clientState;
    }

    public Optional<ClientState> findByCtx(ChannelHandlerContext ctx) {
        return Optional.ofNullable(clientTable.get(ctx));
    }

}
