package com.imooc.controller;

import com.imooc.enums.OperatorFriendRequestTypeEnum;
import com.imooc.enums.SearchFriendsStatusEnum;
import com.imooc.pojo.Users;
import com.imooc.pojo.bo.UserBO;
import com.imooc.pojo.vo.FriendRequestVO;
import com.imooc.pojo.vo.MyFriendsVO;
import com.imooc.pojo.vo.UsersVO;
import com.imooc.service.UserService;
import com.imooc.utils.FastDFSClient;
import com.imooc.utils.FileUtils;
import com.imooc.utils.IMoocJSONResult;
import com.imooc.utils.MD5Utils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.print.attribute.standard.PresentationDirection;
import java.util.List;

@RestController
@RequestMapping("u")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private FastDFSClient fastDFSClient;


    @GetMapping("/hello")
    public String hello(){
        System.out.println("==========================system.out,print hello wixin ");
        return "hello wixin";
    }

    @PostMapping("/registOrLogin")
    public IMoocJSONResult registOrLogin(@RequestBody Users user) throws Exception {
        //0.判断用户名和密码不能为空
        if(StringUtils.isBlank(user.getUsername())
            || StringUtils.isBlank(user.getPassword())){
            return IMoocJSONResult.errorException("用户名或密码不能为空....");
        }

        boolean usernameIsExist = userService.queryUsernameIsExist(user.getUsername());
        Users userResult;
        if(usernameIsExist){
            //1.1 登录
            userResult = userService.queryUserForLogin(user.getUsername(),
                    MD5Utils.getMD5Str(user.getPassword()));
            if(userResult == null){
                return IMoocJSONResult.errorMsg("用户名或密码不正确.....");
            }
        }else{
            //1.2 注册
            user.setNickname(user.getUsername());
            user.setFaceImage("");
            user.setFaceImageBig("");
            user.setPassword(MD5Utils.getMD5Str(user.getPassword()));
            userResult = userService.saveUser(user);
        }

        UsersVO userVO = new UsersVO();
        BeanUtils.copyProperties(userResult, userVO);
        return IMoocJSONResult.ok(userVO);
    }

    @PostMapping("/uploadFaceBase64")
    public IMoocJSONResult uploadFaceBase64(@RequestBody UserBO userBO) throws Exception {

        //获取前端传过来的base64字符串，然后转换为文件对象再上传
        String base64Data = userBO.getFaceData();
        String userFacePath = "D:\\uploadtmp\\" + userBO.getUserId() + "userface64.png";
        FileUtils.base64ToFile(userFacePath, base64Data);
        System.out.println("into uploadFaceBase64 function:  " + userFacePath);

        //上传文件到fastdfs
        MultipartFile faceFile = FileUtils.fileToMultipart(userFacePath);
        String url = fastDFSClient.uploadBase64(faceFile);
        System.out.println("url: " + url);
        //小图 “a_80x80.png"
        //大图 “a.png"

        //获取略缩图的url
        String thump = "_80x80.";
        String arr[] = url.split("\\.");
        String thumpImgUrl = arr[0] + thump + arr[1];

        //更新用户对象
        Users user = new Users();
        user.setId(userBO.getUserId());
        user.setFaceImageBig(url);
        user.setFaceImage(thumpImgUrl);

        //更新用户的信息
        user = userService.updateUserInfo(user);

        return IMoocJSONResult.ok(user);
    }

    /**
     * 设置昵称
     * @param userBO
     * @return
     * @throws Exception
     */
    @PostMapping("/setNickname")
    public IMoocJSONResult setNickname(@RequestBody UserBO userBO) throws Exception {
        Users user = new Users();
        user.setId(userBO.getUserId());
        user.setNickname(userBO.getNickname());
        user = userService.updateUserInfo(user);
        return IMoocJSONResult.ok(user);
    }

    /**
     * 搜索好友接口,根据账号做匹配查询而不是模糊查询
     * @return
     * @throws Exception
     */
    @PostMapping("/search")
    public IMoocJSONResult searchUser(String myUserId, String friendUsername)
            throws Exception {
        //0.判断myUserId friendUsername 不能为空
        if(StringUtils.isBlank(myUserId)
           || StringUtils.isBlank(friendUsername)){
            return IMoocJSONResult.errorMsg("");
        }

        //前置条件  -1.搜索的用户如果不存在，返回【无用户】
        //前置条件  -2.搜索的用户是自己，返回【不能添加自己】
        //前置条件  -1.搜索的用户已经是你的好友，返回【该用户已经是你的好友】
        Integer status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if(status == SearchFriendsStatusEnum.SUCCESS.status){
            Users user = userService.queryUserInfoByUsername(friendUsername);
            UsersVO userVO = new UsersVO();
            BeanUtils.copyProperties(user, userVO);
            return IMoocJSONResult.ok(userVO);
        }else{
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return IMoocJSONResult.errorMsg(errorMsg);
        }
    }

    /**
     * 发送添加好友请求
     * @return
     * @throws Exception
     */
    @PostMapping("/addFriendRequest")
    public IMoocJSONResult addFriendRequest(String myUserId, String friendUsername)
            throws Exception {
        //0.判断myUserId friendUsername 不能为空
        if(StringUtils.isBlank(myUserId)
                || StringUtils.isBlank(friendUsername)){
            return IMoocJSONResult.errorMsg("");
        }

        //前置条件  -1.搜索的用户如果不存在，返回【无用户】
        //前置条件  -2.搜索的用户是自己，返回【不能添加自己】
        //前置条件  -1.搜索的用户已经是你的好友，返回【该用户已经是你的好友】
        Integer status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if(status == SearchFriendsStatusEnum.SUCCESS.status){
            //处理添加的请求
            userService.sendFriendRequest(myUserId, friendUsername);

        }else{
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return IMoocJSONResult.errorMsg(errorMsg);
        }
        return IMoocJSONResult.ok();
    }

    /**
     * 查询添加好友请求
     * @return
     * @throws Exception
     */
    @PostMapping("/queryFriendRequests")
    public IMoocJSONResult queryFriendRequests(String userId){
        //0.判断myUserId friendUsername 不能为空
        if(StringUtils.isBlank(userId)){
            return IMoocJSONResult.errorMsg("");
        }
        //查询用户接收到的朋友申请
        return IMoocJSONResult.ok(userService.queryFriendRequestList(userId));
    }

    /**
     * @Description 接收方通过或忽略朋友请求
     * @return
     * @throws Exception
     */
    @PostMapping("/operFriendRequest")
    public IMoocJSONResult operFriendRequest(String acceptUserId,
                         String sendUserId, Integer operType){
        //0.判断acceptUserId、 sendUserId 不能为空
        if(StringUtils.isBlank(acceptUserId) ||
                StringUtils.isBlank(sendUserId) || operType == null){
            return IMoocJSONResult.errorMsg("");
        }
        //1.如果operType 没有对应的枚举值，则直接抛出空错误信息
        if(StringUtils.isBlank(OperatorFriendRequestTypeEnum.getMsgByType(operType))){
            return IMoocJSONResult.errorMsg("");
        }
        if(operType == OperatorFriendRequestTypeEnum.IGNORE.type){
            //2.如果忽略好友请求，则删除好友请求的数据库表记录
            userService.deleteFriendRequest(sendUserId, acceptUserId);
        }else if(operType == OperatorFriendRequestTypeEnum.PASS.type){
            //3.判断如果是通过好友请求，则互相增加好友记录到数据库对应的表
            //然后删除好友请求的数据库表记录
            userService.passFriendRequest(sendUserId, acceptUserId);
            userService.deleteFriendRequest(sendUserId, acceptUserId);
        }
        // 4.数据库查询好友列表
        List<MyFriendsVO> myFriends = userService.queryMyFriends(acceptUserId);
        return IMoocJSONResult.ok(myFriends);
    }

    /**
     * @Description 查询好友列表
     * @return
     * @throws Exception
     */
    @PostMapping("/myFriends")
    public IMoocJSONResult myFriends(String userId){
        //0. userId判断不能为空
        if(StringUtils.isBlank(userId)){
            return IMoocJSONResult.errorMsg("");
        }
        //1.数据库查询好友列表
        List<MyFriendsVO> myFriends = userService.queryMyFriends(userId);
        System.out.println("myFriends："+ myFriends);
        return IMoocJSONResult.ok(myFriends);
    }
}
