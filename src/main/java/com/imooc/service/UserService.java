package com.imooc.service;

import com.imooc.pojo.Users;
import com.imooc.pojo.vo.FriendRequestVO;
import com.imooc.pojo.vo.MyFriendsVO;
import netty.ChatMsg;

import java.util.List;

public interface UserService {
    /**
     * @Description: 判断用户名是否存在
     *
     * @param username
     * @return
     */
    public boolean queryUsernameIsExist(String username);

    /**@
     * 查询用户是否存在
     * @param username
     * @param pws
     * @return
     */
    public Users queryUserForLogin(String username, String pws);

    /**@]
     *
     * @param user 用户注册
     * @return
     */
    public Users saveUser(Users user);

    /**@]
     *
     * @param user 修改用户记录
     * @return
     */
    public Users updateUserInfo(Users user);

    /**@]
     *
     * @  查询用户信息
     * @return
     */
    public Users queryUserById(String userId);

    /**@]
     *
     * @  搜索朋友的前置条件
     * @return
     */
    public Integer preconditionSearchFriends(String myUserId,
                                             String friendUsername);



    /**@]
     *
     * @  搜索朋友的前置条件
     * @return
     */
    public Users queryUserInfoByUsername(String username);


    /**
     *
     * @  添加好友请求记录，保存到数据库
     * @return
     */
    public void sendFriendRequest(String myUserId, String friendUsername);

    /**
     *
     * @  发送添加好友记录
     * @return
     */
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId);

    /**
     *
     * @  删除请求添加好友记录
     * @return
     */
    public void deleteFriendRequest(String sendUserId, String acceptUserId);

    /**
     *
     * @Description  通过好友请求
     * 1.保存好友
     * 2.逆向保存好友
     * 3.删除请求添加好友记录
     * @return
     */
    public void passFriendRequest(String sendUserId, String acceptUserId);

    /**
     *
     * @Description  查询好友列表
     * 1.保存好友
     * 2.逆向保存好友
     * 3.删除请求添加好友记录
     * @return
     */
    public List<MyFriendsVO> queryMyFriends(String userId);

    /**
     *
     * @Description 保存聊天消息到数据库
     * @return
     */
    public String saveMsg(ChatMsg chatMsg);

    /**
     *
     * @Description 保存聊天消息到数据库
     * @return
     */
    public String updateMsgSigned(List<String> msgIdList);

    /**
     * @Description  获取未签收消息列表
     * @param acceptUserId
     * @return
     */
    public List<com.imooc.pojo.ChatMsg> getUnReadMsgList(String acceptUserId);


}
