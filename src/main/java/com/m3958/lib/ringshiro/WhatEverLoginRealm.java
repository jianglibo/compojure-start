package com.m3958.lib.ringshiro;

import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;


public class WhatEverLoginRealm extends AuthorizingRealm{

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
    Set<String> roles = new HashSet<>();
    roles.add("admin");
    info.setRoles(roles);
    return info;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
      throws AuthenticationException {
    //这里是存储在数据库中的内容。
    SimpleAuthenticationInfo info = new SimpleAuthenticationInfo("a", "b", "myUserRealm");
    return info;
  }
  
}
