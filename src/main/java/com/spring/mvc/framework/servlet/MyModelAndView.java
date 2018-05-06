package com.spring.mvc.framework.servlet;

import java.util.Map;

/**
 * Created by daniel on 2018/5/3.
 */
public class MyModelAndView {

    //页面模板
    private String view;
    //回写的值
    private Map<String,Object> model;

    public MyModelAndView(String view){
        this.view = view;
    }

    public MyModelAndView(String view,Map<String,Object> model){
        this.view = view;
        this.model = model;
    }
    
    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public Map<String, Object> getModel() {
        return model;
    }

    public void setModel(Map<String, Object> model) {
        this.model = model;
    }
}
