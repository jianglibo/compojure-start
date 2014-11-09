package com.m3958.lib.ringshiro;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Hello world!
 * 
 */
public class App {
  private static final transient Logger log = LoggerFactory.getLogger(App.class);

  public static void main(String[] args) throws IOException {
    new App().start();
  }

  private void start() throws IOException {
    initShiro();
    HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
    server.createContext("/", new MyHandler());
    server.setExecutor(null); // creates a default executor
    server.start();
  }
  
  /**
   * shiro 通过类的继承，在不同层级设定默认值。
   */
  private void initShiro() {
    DefaultSecurityManager securityManager = new DefaultSecurityManager(new WhatEverLoginRealm());
    DefaultSessionManager smgr = new DefaultSessionManager();
    smgr.setSessionDAO(new EnterpriseCacheSessionDAO());
    securityManager.setSessionManager(smgr);
    securityManager.setCacheManager(new EhCacheManager());
    SecurityUtils.setSecurityManager(securityManager);
  }
  /**
   * 请求过程如下，第一次请求时，页面返回一个sessionId，但是没有值。然后将这个sessionId添加到url中，比如：j=xxxx-xx-xxx-xxxxx-xxxx，接下来的请求将会出现值，并且
   * sessionId不再改变，你可以中途将sessionId从url中去掉，然后在加上。
   */
  static class MyHandler implements HttpHandler {
    public void handle(HttpExchange t1) throws IOException {
      //SubjectContext 接口定义了getSessionId和setSessionId.
      final String uri = t1.getRequestURI().toString();
      String sid;
      if (uri.length() > 1) {
        sid = uri.split("=")[1];
      } else {
        sid = null;
      }
//      SubjectContext subc = new RingSubjectContext();
//      subc.setSessionId(sid);
      final Subject subject = new Subject.Builder().sessionId(sid).buildSubject();
      final HttpExchange t = t1;
      subject.execute(new Runnable() {
        @Override
        public void run() {
          try {
            if (!subject.isAuthenticated()) {
              subject.login(new UsernamePasswordToken("a", "b"));
            }
            Session sess = subject.getSession(false);
            String svv = "";
            Object sv = sess.getAttribute("a");
            if (sv == null) {
              sess.setAttribute("a", "Yes!");
            } else {
              svv = sv.toString();
            }
            String resp = "sessionValue: " + svv + "\nsessionId: " + sess.getId().toString();
            
            t.sendResponseHeaders(200, resp.length());
            OutputStream os = t.getResponseBody();
            os.write(resp.getBytes());
            os.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });
    }
  }
}
