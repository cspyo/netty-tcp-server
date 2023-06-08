package com.pyo.netty.handler;

import com.pyo.netty.constant.MessageType;
import com.pyo.netty.constant.ResponseType;
import com.pyo.netty.decoder.Sha256Encryptor;
import com.pyo.netty.domain.ClientState;
import com.pyo.netty.repository.ClientRepository;
import com.pyo.netty.repository.RainbowTableRepository;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class CustomHandler extends ChannelInboundHandlerAdapter {

    private final RainbowTableRepository rainbowTableRepository;
    private final ClientRepository clientRepository;

    // 핸들러가 생성될 때 호출되는 메소드
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
    }

    // 핸들러가 제거될 때 호출되는 메소드
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
    }

    // 클라이언트와 연결되어 트래픽을 생성할 준비가 되었을 때 호출되는 메소드
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String remoteAddress = ctx.channel().remoteAddress().toString();
        log.info("Remote Address: " + remoteAddress);
    }

    // 클라이언트로부터 메세지를 수신하면 호출되는 메소드
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg){

        ByteBuf buffer = (ByteBuf) msg;
        int messageLength =  buffer.getByte(0);
        String messageType = Integer.toHexString(buffer.getByte(1) & 0xff);

        // 메세지 타입에 따라 서버의 행동을 정의
        switch (messageType) {
            case MessageType.ENCOUNTER:
                receiveEncounter(buffer, ctx);
                break;
            case MessageType.HELLO:
                receiveHello(ctx);
                break;
            case MessageType.CHECK:
                receiveCheck(buffer, ctx);
                break;
            default:
                log.info("Wrong Message Type Received");
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        ctx.close();
        cause.printStackTrace();
    }

    // ENCOUNTER 메세지를 받았을 때,
    public void receiveEncounter(ByteBuf buffer, ChannelHandlerContext ctx) {
        // LittleEnidan 으로 데이터를 읽어서 byte 배열에 저장
        byte[] receivedMsgBytes = {buffer.getByte(4), buffer.getByte(3)};

        // byte 값을 그대로 String 으로 변환하고 rainbowTable에서 알맞은 값을 찾아서 반환
        String msgString = Sha256Encryptor.bytesToString(receivedMsgBytes);
        String decryptStringValue = rainbowTableRepository.findDecryptValue(msgString);

        // String 값을 다시 byte 배열로 변환
        byte[] decryptBytesValue = Sha256Encryptor.stringToBytes(decryptStringValue);

        // 클라이언트에 보낼 메세지를 LittleEndian byte 배열로 생성
        byte[] toSendClient = {0x02, (byte) 0xa2, 0x00, decryptBytesValue[1], decryptBytesValue[0]};

        // 클라이언트 리포지토리에 클라이언트 상태 저장
        ClientState clientState = new ClientState(true, false, decryptStringValue);
        clientRepository.save(ctx, clientState);

        ByteBuf buf = Unpooled.wrappedBuffer(toSendClient);
        ctx.writeAndFlush(buf);
    }

    // Hello 메세지를 수신했을 때
    public void receiveHello(ChannelHandlerContext ctx) {
        // 기본 메세지는 there
        String clientName = "there";

        // 만약 해당 클라이언트가
        if (clientRepository.findByCtx(ctx).isPresent()) {
            ClientState clientState = clientRepository.findByCtx(ctx).get();
            // 이전에 서버가 Encounter 에 맞는 해싱값을 클라이언트에게 제공했을 경우
            if (clientState.isCheck()) {
                // 이름을 추출하고
                clientName = clientState.getName();
                // 2바이트 값 중 첫번째 8비트가 0이라면, 문자 0 제거
                if ('0' == clientName.charAt(0)) {
                    clientName = clientName.substring(1);
                }
            }
        }

        // 클라이언트의 상태에 어울리는 String 메세지 생성
        String data = "Hi, " + clientName + "!";
        byte[] byteData = data.getBytes(StandardCharsets.UTF_8);

        // 메세지에 맞는 헤더 생성
        byte[] byteHeader = {(byte) byteData.length, (byte) 0xa5, 0x00};

        // 만들어진 Header 배열과 body 배열을 합친다.
        byte[] byteMsg = new byte[byteHeader.length + byteData.length];
        System.arraycopy(byteHeader, 0, byteMsg, 0, byteHeader.length);
        System.arraycopy(byteData, 0, byteMsg, byteHeader.length, byteData.length);

        // ByteBuf 객체로 생성하여 클라이언트에 전송
        ByteBuf buf = Unpooled.wrappedBuffer(byteMsg);
        ctx.writeAndFlush(buf);
    }


    // Check 메세지를 수신했을 때
    public void receiveCheck(ByteBuf buffer, ChannelHandlerContext ctx) {
        String responseType = Integer.toHexString(buffer.getByte(3) & 0xff);

        // Encounter 에 대한 해싱값이 올바를 경우, 클라이언트에서 success 메세지를 보낸다.
        if (responseType.equals(ResponseType.SUCCESS)) {
            // clientRepository 에서 현재 클라이언트의 상태를 찾고
            ClientState clientState = clientRepository.findByCtx(ctx).get();
            // check 값을 True 로 변경한다.
            // 이후에 이 클라이언트가 Hello 메세지를 보내면 이전에 해싱한 값으로 대신 인사한다.
            // Hi, 505!
            clientState.setCheck(true);
        }
    }

}
