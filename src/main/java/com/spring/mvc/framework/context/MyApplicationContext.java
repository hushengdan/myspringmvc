package com.spring.mvc.framework.context;


import com.spring.mvc.framework.annotation.MyAutowired;
import com.spring.mvc.framework.annotation.MyController;
import com.spring.mvc.framework.annotation.MyService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by daniel on 2018/5/3.
 */
public class MyApplicationContext {
    
    private Map<String,Object> instanceMapping = new ConcurrentHashMap<String,Object>();
    
    private List<String> classCache = new ArrayList<>();

    private Properties properties = new Properties();

    public Properties getProperties() {
        return properties;
    }

    public MyApplicationContext(String location){
        InputStream is = null;
        try{
            location = location.replace("classpath:","");
            
            //1.定位
            is = this.getClass().getClassLoader().getResourceAsStream(location);
            //2.载入
            properties.load(is);
            //3.注册,保存所有class
            doRegister(properties.getProperty("scanPackage"));
            //4.实例化，加了service,controller注解的对象
            doCreateBean();
            //5.注入
            populate();
        }catch (IOException e){
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("IOC init success!!!");
    }

    //把所有符合条件的class注册到缓存中
    private void doRegister(String packageName) {
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()){
            //如果是文件夹，递归继续查找
            if(file.isDirectory()){
                doRegister(packageName + "." + file.getName());
            }else{
                classCache.add(packageName + "." + file.getName().replace(".class","").trim());
            }
        }
    }

    private void doCreateBean(){
        if(classCache.size()==0) return;
        try{
            for ( String className : classCache){
                //spring会判断用jdk还是cglib
                Class<?> clazz = Class.forName(className);
                //只要加了service controller注解的都要初始化
                if(clazz.isAnnotationPresent(MyController.class)){
                    String id = lowerFirstChar(clazz.getSimpleName());
                    instanceMapping.put(id,clazz.newInstance());
                }else if(clazz.isAnnotationPresent(MyService.class)){
                    MyService service = clazz.getAnnotation(MyService.class);
                    //优先使用已经定义的名字
                    String id = service.value();
                    if(!"".equals(id.trim())){
                        instanceMapping.put(id,clazz.newInstance());
                        continue;
                    }
                    
                    //如果是接口，根据默认类型匹配
                    Class<?>[] interfaces = clazz.getInterfaces();
                    if(interfaces != null){
                        for (Class<?> i : interfaces){
                            instanceMapping.put(i.getName(),clazz.newInstance());
                        }
                    }
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void populate() {
        if(instanceMapping == null) return;
        for (Map.Entry<String,Object> entry : instanceMapping.entrySet()){
            //取出所有属性并注入
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields){
                if(!field.isAnnotationPresent(MyAutowired.class)) continue;
                MyAutowired autowired = field.getAnnotation(MyAutowired.class);
                String id = autowired.value().trim();
                if("".equals(id.trim())){
                    id = field.getType().getName();
                }
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(),instanceMapping.get(id));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    //首字母转成小写
    private String lowerFirstChar(String str){
        if(str == null) return "";
        char[] chars = str.toCharArray();
        chars[0]+=32;
        return String.valueOf(chars);
    }
    
    public Object getBean(String name){
        return instanceMapping.get(name);
    }
    
    public Map<String,Object> getAll(){
        return this.instanceMapping;
    }
}
