package netty;


import java.io.Serializable;

public class ChatMsg implements Serializable {

    private static final long serialVersionUID = -6174669566018346601L;

    private String senderId;     // 发送者用户Id
    private String receiverId;     // 接受者用户Id
    private String msg;     //  聊天内容
    private String msgId;    // 用于消息的签收

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }
}
