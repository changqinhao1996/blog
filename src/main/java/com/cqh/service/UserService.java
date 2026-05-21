package com.cqh.service;

import com.cqh.po.User;

public interface UserService {

    User checkUser(String username, String password);
}
