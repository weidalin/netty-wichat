package netty;

import com.imooc.SpringUtil;
import com.imooc.enums.MsgActionEnum;
import com.imooc.service.UserService;
import com.imooc.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 处理消息的handler
 * TextWebSocketFrame:在netty中，用于为websocket专门处理文本的对象，frame是消息的载体
 */
public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private static ChannelGroup users = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                TextWebSocketFrame msg) throws Exception {
        //获取客户端传输过来的消息
        String content = msg.text();

        // 1. 获取客户端发来的消息
        DataContent dataContent = JsonUtils.jsonToPojo(content, DataContent.class);
        Integer action = dataContent.getAction();

        Channel currentChannel = ctx.channel();
        // 2. 判断消息类型，根据不同类型处理不同的业务
        if(action == MsgActionEnum.CONNECT.type){
            //  2.1 当websocket第一次open的时候，初始化channel，把用户的channel和userId关联起来
            String senderId = dataContent.getChatMsg().getSenderId();
            UserChannelRel.put(senderId, currentChannel);
            // 测试
            for(Channel c : users){
                System.out.println(c.id().asLongText());
            }
            UserChannelRel.output();

        }else if (action == MsgActionEnum.CHAT.type) {
            //  2.2 聊天类型的消息，把聊天记录保存到数据库，同时标记消息的签收状态[未签收]
            ChatMsg chatMsg = dataContent.getChatMsg();
            String msgText = chatMsg.getReceiverId();
            String senderId = chatMsg.getSenderId();
            String receiverId = chatMsg.getReceiverId();

            // 保存消息到数据库，并且标记为未签收
            UserService userService = (UserService) SpringUtil.getBean("userServiceImpl");
            String msgId = userService.saveMsg(chatMsg);
            chatMsg.setMsgId(msgId);

            DataContent dataContentMsg = new DataContent();
            dataContentMsg.setChatMsg(chatMsg);

            // 发送消息
            // 从全局用户Channel关系中获取接受放的channel
            Channel receiveChannel = UserChannelRel.get(receiverId);
            if(receiveChannel == null){
                // TODO channel 为空，表示用户离线，推送消息（JPush, 个推， 小米推）

            }else{
                //当receiveChannel不为空的时候，从channelGroup去查找对应的channel是否存在
                Channel findChannel = users.find(receiveChannel.id());
                if(findChannel != null){
                    //用户在线
                    receiveChannel.writeAndFlush(new TextWebSocketFrame(
                            JsonUtils.objectToJson(dataContentMsg)
                    ));
                }else{
                    // 用户离线， TODO 离线推送
                }
            }


        }else if (action == MsgActionEnum.SIGNED.type) {
            //  2.3 签收消息类型，针对具体的消息进行签收，修改数据库中对应消息的签收状态[已签收]
            //
            UserService userService = (UserService) SpringUtil.getBean("userServiceImpl");
            // 扩展字段在signed类型消息中， 代表需要去签收的消息id,逗号间隔
            String  msgIdStr = dataContent.getExtand();
            String[] masIds = msgIdStr.split(",");
            List<String> msgIdList = new ArrayList<>();
            for(String mid : masIds){
                if(StringUtils.isNoneBlank(mid)){
                    msgIdList.add(mid);
                }
            }
            System.out.println(msgIdList.toString());
            if(msgIdList != null && !msgIdList.isEmpty() && msgIdList.size() > 0){
                //批量签收
                userService.updateMsgSigned(msgIdList);
            }
        }else if (action == MsgActionEnum.KEEPALIVE.type) {
            //  2.4 心跳类型的消息
        }




        /* 注释掉 hello-netty
        System.out.println("接收到的数据：" + content);

        for(Channel channel : users){
            channel.writeAndFlush(new TextWebSocketFrame("[服务器接收到消息： ]" +
                    LocalDateTime.now() + "接收到消息，消息为： " + content));
        }
        //下面的与上面的一样
//        clients.writeAndFlush(
//                new TextWebSocketFrame(("[服务器接收到消息： ]" +
//                        LocalDateTime.now() + "接收到消息，消息为： " + content)));

         */



    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        /**
         * 当客户端连接服务器之后（打开连接）
         * 获取客户端的channel,并且放到ChannelGroup中进行管理
         */
        users.add(ctx.channel());

    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        /**
         * 当客户端断开连接服务器之后（断开连接）
         *
         */
        users.remove(ctx.channel());
        System.out.println("客户端断开，channel对应的长id为：" +
                ctx.channel().id().asLongText());
        System.out.println("客户端断开，channel对应的短id为：" +
                ctx.channel().id().asShortText());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        //发生异常之后关闭连接（关闭channel), 随后从ChaennelGroup中移除
        ctx.channel().close();
        users.remove(ctx.channel());
    }


}
