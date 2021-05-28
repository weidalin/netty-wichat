package netty.netty_hello;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * 这是一个初始化其，channel注册之后，会执行里面的相应的初始化方法
 */
public class HelloServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        //通过socketChannel去获得对应的管道
        ChannelPipeline pipeline = socketChannel.pipeline();

        //通过管道添加handler
        //HttpServerCodec是由netty自己提供的助手类，也可以理解为拦截器
        //当请求到服务端，我们需要做解码，相应到客户端做编码
        pipeline.addLast("HttpServerCodedec", new HttpServerCodec());

        //添加一个自定义的Handler
        pipeline.addLast("customHandler", new CustomHandler());
    }
}
