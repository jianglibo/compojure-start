package com.m3958.lib.ringshiro;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StateOb {
  private String astring;
  private int aint;
  
  private Map<String, String> strmap;
  
  private List<String> strlist;
  
  private Set<String> strset;
  
  public StateOb() {
    strmap = new HashMap<>();
    strmap.put("a", "1");
    strmap.put("b", "2");
    strmap.put("c", "3");
    new String(new char[]{'a'});
    strlist = Arrays.asList("a", "b", "c");
    strset = new HashSet<>(strlist);
  }
  
  public String getAstring() {
    return astring;
  }
  public void setAstring(String astring) {
    this.astring = astring;
  }
  public int getAint() {
    return aint;
  }
  public void setAint(int aint) {
    this.aint = aint;
  }

  public Map<String, String> getStrmap() {
    return strmap;
  }

  public void setStrmap(Map<String, String> strmap) {
    this.strmap = strmap;
  }

  public List<String> getStrlist() {
    return strlist;
  }

  public void setStrlist(List<String> strlist) {
    this.strlist = strlist;
  }

  public Set<String> getStrset() {
    return strset;
  }

  public void setStrset(Set<String> strset) {
    this.strset = strset;
  }
  
}
