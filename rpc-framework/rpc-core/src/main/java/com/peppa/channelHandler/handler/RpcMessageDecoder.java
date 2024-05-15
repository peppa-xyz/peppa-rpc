package com.peppa.channelHandler.handler;

import com.peppa.transport.message.MessageFormatConstant;
import com.peppa.transport.message.RequestPayload;
import com.peppa.transport.message.RpcRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

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
 *
 * @author: peppa
 * @create: 2024-05-15 21:08
 **/
@Slf4j
public class RpcMessageDecoder extends LengthFieldBasedFrameDecoder {
    public RpcMessageDecoder() {
        super(
                // 找到当前报文的总长度，截取报文，然后进行解析
                // 最大帧的长度
                MessageFormatConstant.MAX_FRAME_LENGTH,
                // 长度字段的偏移量
                MessageFormatConstant.MAGIC.length + MessageFormatConstant.VERSION_LENGTH + MessageFormatConstant.HEADER_FIELD_LENGTH,
                // 长度字段的长度
                MessageFormatConstant.FULL_FIELD_LENGTH,
                // todo 长度字段的调整值 负载的适配长度 (除去多少个首部字节，剩下的全是消息体)
                -(MessageFormatConstant.MAGIC.length
                        + MessageFormatConstant.VERSION_LENGTH
                        + MessageFormatConstant.HEADER_FIELD_LENGTH
                        + MessageFormatConstant.FULL_FIELD_LENGTH),
                // 需要跳过的字节数
                0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        Object decode = super.decode(ctx, in);

        if (decode instanceof ByteBuf byteBuf) {
            return decodeFrame(byteBuf);
        }
        return null;
    }

    private Object decodeFrame(ByteBuf byteBuf) {
        byte[] magic = new byte[MessageFormatConstant.MAGIC.length];
        byteBuf.readBytes(magic);
        // 魔数校验
        for (int i = 0; i < magic.length; i++) {
            if (magic[i] != MessageFormatConstant.MAGIC[i]) {
                throw new RuntimeException("魔数不匹配,不合法的协议包");
            }
        }

        // 2. 解析版本号
        byte version = byteBuf.readByte();
        if (version > MessageFormatConstant.VERSION) {
            throw new RuntimeException("获得的请求版本不被支持");
        }

        // 3. 解析header长度
        short headerLength = byteBuf.readShort();

        // 4. 解析full length
        int fullLength = byteBuf.readInt();

        // 5. 解析请求类型 todo 判断是不是心跳检测
        byte requestType = byteBuf.readByte();

        // 6. 解析序列化类型
        byte serializeType = byteBuf.readByte();

        // 7. 解析压缩类型
        byte compressType = byteBuf.readByte();

        // 封装一个 RpcRequest
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setRequestType(requestType);
        rpcRequest.setCompressType(compressType);
        rpcRequest.setSerializeType(serializeType);

        // todo 心跳请求没有负载，此处可以判断并直接返回
        if ( requestType == 2) {
            return rpcRequest;
        }


        // 8. 解析requestId
        long requestId = byteBuf.readLong();

        // 9. 解析body
        int payLoadLength = fullLength - headerLength;
        byte[] payLoad = new byte[payLoadLength];
        byteBuf.readBytes(payLoad);

        // 有了字节数组之后就可以解压缩，反序列化
        // todo 解压缩

        // todo 反序列化
        try (ByteArrayInputStream bis = new ByteArrayInputStream(payLoad);
             ObjectInputStream ois = new ObjectInputStream(bis);
        ) {
            RequestPayload requestPayload = (RequestPayload) ois.readObject();
            rpcRequest.setRequestPayload(requestPayload);
        } catch (IOException | ClassNotFoundException e) {
            log.error("请求【{}】反序列化时发生了异常", requestId, e);
        }
        return rpcRequest;
    }
}
