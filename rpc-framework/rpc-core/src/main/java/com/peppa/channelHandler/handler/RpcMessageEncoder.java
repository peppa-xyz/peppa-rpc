package com.peppa.channelHandler.handler;

import com.peppa.transport.message.MessageFormatConstant;
import com.peppa.transport.message.RequestPayload;
import com.peppa.transport.message.RpcRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 自定义协议编码器
 * <p>
 * <pre>
 *   0    1    2    3    4    5    6    7    8    9    10   11   12   13   14   15   16   17   18   19   20   21   22   23
 *   +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
 *   |    magic               |ver |head  len|    full length    | qt | ser|comp|              RequestId                |
 *   +-----+-----+-------+----+----+----+----+-----------+----- ---+--------+----+----+----+----+----+----+---+---+----+
 *   |                                                                                                                  |
 *   |                                         body                                                                     |
 *   |                                                                                                                  |
 *   +--------------------------------------------------------------------------------------------------------+---+----+
 * </pre>
 * <p>
 * 5B magic(魔数)   --->peppa.getBytes()
 * 1B version(版本)   ----> 1
 * 2B header length 首部的长度
 * 4B full length 报文总长度
 * 1B serialize
 * 1B compress
 * 1B requestType
 * 8B requestId
 * <p>
 * body
 * <p>
 * 出站时第一个经过的处理器
 *
 * @author: peppa
 * @create: 2024-05-15 20:06
 **/
@Slf4j
public class RpcMessageEncoder extends MessageToByteEncoder<RpcRequest> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RpcRequest rpcRequest, ByteBuf byteBuf) throws Exception {
        // 5个字节的魔数值
        byteBuf.writeBytes(MessageFormatConstant.MAGIC);
        // 1个字节的版本号
        byteBuf.writeByte(MessageFormatConstant.VERSION);
        // 2个字节的头部长度
        byteBuf.writeShort(MessageFormatConstant.HEADER_LENGTH);
        // 4个字节的报文总长度，但是目前还不知道 使用writerIndex(写指针) 先跳过不写 将写指针移动到 总长度字段所占字节 之后
        byteBuf.writerIndex(byteBuf.writerIndex() + 4);
        // 1个字节的请求类型
        byteBuf.writeByte(rpcRequest.getRequestType());
        // 1个字节的序列化类型
        byteBuf.writeByte(rpcRequest.getSerializeType());
        // 1个字节的压缩类型
        byteBuf.writeByte(rpcRequest.getCompressType());
        // 8个字节的请求id
        byteBuf.writeLong(rpcRequest.getRequestId());

        // 写入请求体 requestPayload
        byte[] body = getBodyBytes(rpcRequest.getRequestPayload());
        byteBuf.writeBytes(body);

        // 重新处理报文的总长度
        // 先保存当前写指针的位置
        int writerIndex = byteBuf.writerIndex();
        // 将写指针移动到 总长度字段 所在位置
        byteBuf.writerIndex(8);

        // 将总长度写入
        byteBuf.writeInt(MessageFormatConstant.HEADER_LENGTH + body.length);

        // 恢复写指针
        byteBuf.writerIndex(writerIndex);


    }

    private byte[] getBodyBytes(RequestPayload requestPayload) {
        // todo 针对不同的消息类型需要做不同处理，心跳的请求，没有payLoad

        // 希望通过一些设计模式，面向对象编程，让我们可以配置修改序列化和压缩的方式
        // 对象怎么变成一个字节数组 序列化 压缩
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(baos);
            objectOutputStream.writeObject(requestPayload);
            // 压缩

            return baos.toByteArray();
        } catch (IOException e) {
            log.error("序列化时出现异常");
            throw new RuntimeException(e);
        }

    }
}
