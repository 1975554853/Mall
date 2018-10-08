package com.bsb.web.controller.portal;

import com.bsb.common.Const;
import com.bsb.common.ResponseCode;
import com.bsb.common.ServerResponse;
import com.bsb.util.CookieUtil;
import com.bsb.util.JsonUtil;
import com.bsb.util.RedisUtilFactory;
import com.bsb.web.pojo.User;
import com.bsb.web.service.IUserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author zeng
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private IUserService userService;
    @Autowired
    private RedisUtilFactory redisUtilFactory;

    /**
     * 用户登录
     *
     * @param username
     * @param password
     * @return
     */
    @PostMapping("/login")
    public ServerResponse<User> login(String username, String password, HttpSession session, HttpServletResponse response) {

        ServerResponse<User> serverResponse = userService.login(username, password);
        if (serverResponse.isSuccess()) {
            CookieUtil.writeLoginToken(response, session.getId());
            redisUtilFactory.setRedisValueEx(session.getId(), JsonUtil.objToString(serverResponse.getData()),
                    Const.RedisCacheExTime.REDIS_SESSION_EXTIME);
        }
        return serverResponse;
    }

    @PostMapping("/logout")
    public ServerResponse<String> logout(HttpServletRequest request, HttpServletResponse response) {
        String loginToken = CookieUtil.readLoginToken(request);
        CookieUtil.delLoginToken(request, response);
        redisUtilFactory.delete(loginToken);
        return ServerResponse.createBySuccessMsg("注销成功");
    }

    @PostMapping("/register")
    public ServerResponse<String> register(User user) {

        return userService.register(user);
    }

    @PostMapping("/checkValid")
    public ServerResponse<String> checkValid(String str, String type) {
        return userService.checkValid(str, type);
    }

    @GetMapping("/userInfo")
    public ServerResponse<User> getUserInfo(HttpServletRequest request) {

        String loginToken = CookieUtil.readLoginToken(request);
        if (StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMsg("用户未登录");
        }
        String userJson = redisUtilFactory.getRedisValue(loginToken);
        User user = JsonUtil.stringToObj(userJson, User.class);
        user.setPassword(StringUtils.EMPTY);

        return ServerResponse.createBySuccess(user);
    }

    @GetMapping("/getQuestion/{username}")
    public ServerResponse<String> forgetGetQuestion(@PathVariable String username) {

        return userService.getQuestion(username);
    }

    @PostMapping("/checkQuestion")
    public ServerResponse<String> forgetCheckAnswer(String username, String question, String answer) {

        return userService.checkAnswer(username, question, answer);
    }

    @PostMapping("/forgetResetPassword")
    public ServerResponse<String> forgetResetPassword(String username, String newPassword, String forgetToken) {

        return userService.forgetResetPassword(username, newPassword, forgetToken);
    }

    @PostMapping("/resetPassword")
    public ServerResponse<String> resetPassword(HttpSession session, String oldPassword, String newPassword) {

        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorMsg("用户未登录");
        }

        return userService.resetPassword(user, oldPassword, newPassword);
    }

    @PostMapping("/updateInfo")
    public ServerResponse<User> updateInfo(HttpSession session, User user) {

        User currentUser = (User) session.getAttribute(Const.CURRENT_USER);
        if (currentUser == null) {
            return ServerResponse.createByErrorMsg("用户未登录");
        }

        user.setId(currentUser.getId());
        user.setUsername(currentUser.getUsername());

        ServerResponse<User> response = userService.updateInfo(user);
        if (response.isSuccess()) {
            session.setAttribute(Const.CURRENT_USER, response.getData());
        }

        return response;
    }

    @GetMapping("/getInfo")
    public ServerResponse<User> getInfo(HttpSession session) {

        User currentUser = (User) session.getAttribute(Const.CURRENT_USER);
        if (currentUser == null) {
            return ServerResponse.createByErrorCodeMsg(ResponseCode.NEED_LOGIN.getCode(), "未登录，需要强制登录");
        }

        return userService.getInfo(currentUser.getId());
    }
}
