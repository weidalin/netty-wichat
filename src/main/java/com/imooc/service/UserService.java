package com.imooc.service;

import com.imooc.pojo.Users;

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

}
