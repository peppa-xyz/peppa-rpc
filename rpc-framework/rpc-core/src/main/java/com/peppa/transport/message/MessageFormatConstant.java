package com.peppa.transport.message;

import java.nio.charset.StandardCharsets;

/**
 自定义协议编码器
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
 * @author: peppa
 * @create: 2024-05-15 20:14
 **/
public class MessageFormatConstant {
    // 魔数
    public final static byte[] MAGIC = "peppa".getBytes(StandardCharsets.UTF_8);

    // 协议版本号
    public final static byte VERSION = 1;

    // 首部长度字段所占用的字节数
    public final static short HEADER_LENGTH = (byte) (MAGIC.length + 1 + 2 + 4 + 1 + 1 + 1 + 8);

    // 最大帧的长度
    public final static int MAX_FRAME_LENGTH = 1024 * 1024;

    // 版本号字段所占字节数
    public static final int VERSION_LENGTH = 1;

    // 消息头长度字段所占字节数
    public static final int HEADER_FIELD_LENGTH = 2;

    // 消息体长度字段所占字节数
    public static final int FULL_FIELD_LENGTH = 4;
}
