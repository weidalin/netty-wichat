package com.imooc.service.impl;

import com.imooc.enums.SearchFriendsStatusEnum;
import com.imooc.mapper.FriendsRequestMapper;
import com.imooc.mapper.MyFriendsMapper;
import com.imooc.mapper.UsersMapper;
import com.imooc.mapper.UsersMapperCustom;
import com.imooc.pojo.FriendsRequest;
import com.imooc.pojo.MyFriends;
import com.imooc.pojo.Users;
import com.imooc.pojo.vo.FriendRequestVO;
import com.imooc.pojo.vo.MyFriendsVO;
import com.imooc.service.UserService;
import com.imooc.utils.FastDFSClient;
import com.imooc.utils.FileUtils;
import com.imooc.utils.QRCodeUtils;
import org.n3r.idworker.Sid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.entity.Example.Criteria;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private UsersMapperCustom usersMapperCustom;

    @Autowired
    private MyFriendsMapper myFriendsMapper;

    @Autowired
    private FriendsRequestMapper friendsRequestMapper;

    @Autowired
    private Sid sid;

    @Autowired
    private QRCodeUtils qrCodeUtils;

    @Autowired
    private FastDFSClient fastDFSClient;

    @Transactional(propagation = Propagation.SUPPORTS) //事务
    @Override
    public boolean queryUsernameIsExist(String username) {
        Users user = new Users();
        user.setUsername(username);
        Users result = usersMapper.selectOne(user);

        return result != null ? true : false;
    }


    @Transactional(propagation = Propagation.SUPPORTS) //事务
    @Override
    public Users queryUserForLogin(String username, String pwd) {
        Example userExample = new Example(Users.class);
        Criteria criteria = userExample.createCriteria();
        criteria.andEqualTo("username", username);
        criteria.andEqualTo("password", pwd);

        Users result = usersMapper.selectOneByExample(userExample);
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRED) //事务
    @Override
    public Users saveUser(Users user) {
        String userId =  sid.nextShort();

        //为每个用户生成一个唯一的二维码
        //wixin_qrcode:[username]
        String qrCodePath = "D://uploadtmp/user" + userId + "qrcode.png";
        qrCodeUtils.createQRCode(qrCodePath, "wixin_qrcode:"+user.getUsername());
        MultipartFile qrCodeFile = FileUtils.fileToMultipart(qrCodePath);
        String qrCodeUrl = "";
        try {
            qrCodeUrl = fastDFSClient.uploadQRCode(qrCodeFile);
        } catch(IOException e){
            e.printStackTrace();
        }

        user.setQrcode(qrCodeUrl);
        user.setId(userId);
        usersMapper.insert(user);
        return user;
    }

    @Transactional(propagation = Propagation.REQUIRED) //事务
    @Override
    public Users updateUserInfo(Users user) {
        usersMapper.updateByPrimaryKeySelective(user); //user中为空的属性不去更新， 不会使用null覆盖原有的值
//        usersMapper.updateByPrimaryKey(user); user必须包含主键，根据主键，所有属性都将去更新，会使用null覆盖原有的值
        return queryUserById(user.getId());
    }

    @Transactional(propagation = Propagation.SUPPORTS) //事务
    @Override
    public Users queryUserById(String userId) {
        return usersMapper.selectByPrimaryKey(userId);
    }

    @Transactional(propagation = Propagation.SUPPORTS) //事务
    @Override
    public Integer preconditionSearchFriends(String myUserId, String friendUsername) {
        //1.搜索的用户如果不存在，返回【无用户】
        Users user = queryUserInfoByUsername(friendUsername);
        if(user == null){
            return SearchFriendsStatusEnum.USER_NOT_EXIST.status;
        }
        //2.搜索的用户是自己，返回【不能添加自己】
        if(user.getId().equals(myUserId)){
            return SearchFriendsStatusEnum.NOT_YOURSELF.status;
        }
        //3.搜索的用户已经是你的好友，返回【该用户已经是你的好友】
        Example mfe  = new Example(MyFriends.class);
        Criteria myc = mfe.createCriteria();
        myc.andEqualTo("myUserId", myUserId);
        myc.andEqualTo("myFriendUserId", user.getId());
        MyFriends myFriendsRel = myFriendsMapper.selectOneByExample(mfe);
        if(myFriendsRel != null){
            return SearchFriendsStatusEnum.ALREADY_FRIENDS.status;
        }
        return SearchFriendsStatusEnum.SUCCESS.status;
    }

    @Transactional(propagation = Propagation.SUPPORTS) //事务
    @Override
    public Users queryUserInfoByUsername(String username){
        Example ue  = new Example(Users.class);
        Criteria uc = ue.createCriteria();
        uc.andEqualTo("username", username);
        return usersMapper.selectOneByExample(ue);
    }


    @Override
    public void sendFriendRequest(String myUserId, String friendUsername) {
        //根据用户名把朋友信息查询出来
        Users friend = queryUserInfoByUsername(friendUsername);
        //1.查询发送好友请求记录表
        Example fre = new Example(FriendsRequest.class);
        Criteria frc = fre.createCriteria();
        frc.andEqualTo("sendUserId", myUserId);
        frc.andEqualTo("acceptUserId", friend.getId());
        FriendsRequest friendsRequest = friendsRequestMapper.selectOneByExample(fre);
        if(friendsRequest == null){
            //如果不是你的好友，并且好友记录没有添加，则新增好友记录
            String requestId = sid.nextShort();
            FriendsRequest request = new FriendsRequest();
            request.setId(requestId);
            request.setSendUserId(myUserId);
            request.setAcceptUserId(friend.getId());
            request.setRequestDataTime(new Date());
            friendsRequestMapper.insert(request);
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS) //事务
    @Override
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId) {
        return usersMapperCustom.queryFriendRequestList(acceptUserId);
    }

    @Transactional(propagation = Propagation.SUPPORTS) //事务
    @Override
    public void deleteFriendRequest(String sendUserId, String acceptUserId) {
        Example fre = new Example(FriendsRequest.class);
        Criteria frc = fre.createCriteria();
        frc.andEqualTo("sendUserId", sendUserId);
        frc.andEqualTo("acceptUserId", acceptUserId);
        friendsRequestMapper.deleteByExample(fre);
    }

    @Transactional(propagation = Propagation.REQUIRED) //事务
    @Override
    public void passFriendRequest(String sendUserId, String acceptUserId) {
        saveFriends(sendUserId, acceptUserId);
        saveFriends(acceptUserId, sendUserId);
        deleteFriendRequest(sendUserId, acceptUserId);

    }
    @Transactional(propagation = Propagation.REQUIRED) //事务
    private void saveFriends(String sendUserId, String acceptUserId){
        MyFriends myFriends = new MyFriends();
        String recordId = sid.nextShort();
        myFriends.setId(recordId);
        myFriends.setMyFriendUserId(acceptUserId);
        myFriends.setMyUserId(sendUserId);
        myFriendsMapper.insert(myFriends);

    }


    @Transactional(propagation = Propagation.REQUIRED) //事务
    @Override
    public List<MyFriendsVO> queryMyFriends(String userId) {
        List<MyFriendsVO> myFriends = usersMapperCustom.queryMyFriends(userId);
        return myFriends;
    }
}
