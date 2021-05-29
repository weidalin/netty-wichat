package com.imooc.service.impl;

import com.imooc.mapper.UsersMapper;
import com.imooc.pojo.Users;
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

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UsersMapper usersMapper;

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
}
