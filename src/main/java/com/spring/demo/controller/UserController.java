package com.spring.demo.controller;

import com.spring.demo.service.IOrderService;
import com.spring.demo.service.IUserService;
import com.spring.mvc.framework.annotation.MyAutowired;
import com.spring.mvc.framework.annotation.MyController;
import com.spring.mvc.framework.annotation.MyRequestMapping;
import com.spring.mvc.framework.annotation.MyRequestParam;
import com.spring.mvc.framework.servlet.MyModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by daniel on 2018/5/4.
 */
@MyController
@MyRequestMapping("/user")
public class UserController {

    @MyAutowired
    private IUserService userService;
    
    @MyAutowired("myOrderService")
    private IOrderService orderService;

    @MyRequestMapping("/query/.*.do")
    public MyModelAndView query(HttpServletResponse response,
                                @MyRequestParam("name") String name,
                                @MyRequestParam("addr") String addr) throws IOException {
        System.out.println("name======="+name);
        Map<String, Object> model = new HashMap<>();
        model.put("name", name);
        model.put("addr", addr);
        return new MyModelAndView("index.myhtml",model);
//        out(response, name);
//        return null;
    }
    
    private void out(HttpServletResponse response,String name) throws IOException {
        response.getWriter().write("param is "+name+"");
    }
}
