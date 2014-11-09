package com.m3958.lib.ringshiro;

import java.util.concurrent.Callable;


public class AcallableExecutor {

  public <V> V execute(Callable<V> callable) throws Exception {
    return callable.call();
  }
}
