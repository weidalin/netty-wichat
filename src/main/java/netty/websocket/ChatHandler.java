package netty.websocket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.time.LocalDateTime;

/**
 * 处理消息的handler
 * TextWebSocketFrame:在netty中，用于为websocket专门处理文本的对象，frame是消息的载体
 */
public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private static ChannelGroup clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                TextWebSocketFrame msg) throws Exception {
        //获取客户端传输过来的消息
        String content = msg.text();
        System.out.println("接收到的数据：" + content);

        for(Channel channel : clients){
            channel.writeAndFlush(new TextWebSocketFrame("[服务器接收到消息： ]" +
                    LocalDateTime.now() + "接收到消息，消息为： " + content));
        }
        //下面的与上面的一样
//        clients.writeAndFlush(
//                new TextWebSocketFrame(("[服务器接收到消息： ]" +
//                        LocalDateTime.now() + "接收到消息，消息为： " + content)));


    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        /**
         * 当客户端连接服务器之后（打开连接）
         * 获取客户端的channel,并且放到ChannelGroup中进行管理
         */
        clients.add(ctx.channel());

    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        /**
         * 当客户端断开连接服务器之后（断开连接）
         *
         */
        clients.remove(ctx.channel());
        System.out.println("客户端断开，channel对应的长id为：" +
                ctx.channel().id().asLongText());
        System.out.println("客户端断开，channel对应的短id为：" +
                ctx.channel().id().asShortText());

    }
}
