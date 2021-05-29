package com.imooc.controller;

import com.imooc.pojo.Users;
import com.imooc.pojo.bo.UserBO;
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

    @PostMapping("/setNickname")
    public IMoocJSONResult setNickname(@RequestBody UserBO userBO) throws Exception {
        Users user = new Users();
        user.setId(userBO.getUserId());
        user.setNickname(userBO.getNickname());
        user = userService.updateUserInfo(user);
        return IMoocJSONResult.ok(user);
    }

    }
