package com.spring.mvc.framework.servlet;

import com.spring.mvc.framework.annotation.MyController;
import com.spring.mvc.framework.annotation.MyRequestMapping;
import com.spring.mvc.framework.annotation.MyRequestParam;
import com.spring.mvc.framework.context.MyApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by daniel on 2018/5/3.
 */
public class MyDispatcherServlet extends HttpServlet {
    
    private static final String LOCATION = "contextConfigLocation";
    
    //字符串匹配，不能匹配通配符
    //private Map<String,MyHandler> handlerMapping = new HashMap<>();
    //正则匹配，可以匹配通配符
    //private Map<Pattern,MyHandler> handlerMapping = new HashMap<>();
    //handlerMapping改为list依然可以实现
    private List<MyHandler> handlerMapping = new ArrayList<>();
    
    private Map<MyHandler,MyHandlerAdapter> adapterMapping = new HashMap<>();
    
    private List<ViewResolver> viewResolvers = new ArrayList<>();
    /**
     * 初始化IOC容器
     * @param config
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        //IOC容器初始化
        MyApplicationContext context = new MyApplicationContext(config.getInitParameter(LOCATION));
        Map<String, Object> ioc = context.getAll();
        System.out.println(ioc);
        System.out.println(ioc.get("userController"));
        System.out.println("================================");
        //请求解析
        initMultipartResolver(context);
        //多语言、国际化
        initLocaleResolver(context);
        //主题View层的
        initThemeResolver(context);

        //============== 重要部分 ================
        //解析url和Method的关联关系
        initHandlerMappings(context);
        //适配器（匹配的过程）
        initHandlerAdapters(context);
        //============== 重要部分 ================

        //异常解析
        initHandlerExceptionResolvers(context);
        //视图转发（根据视图名字匹配到一个具体模板）
        initRequestToViewNameTranslator(context);

        //解析模板中的内容（拿到服务器传过来的数据，生成HTML代码）
        initViewResolvers(context);

        initFlashMapManager(context);
        
        System.out.println("MySpringMVC init success!");
    }

    /**
     * 调用controller的方法
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try{
            doDispatch(req,resp);
        }catch (Exception e){
            resp.getWriter().write("Exception,Msg:"+ Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        this.doPost(req, resp);
    }


    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception{
        //从handMapping中取出一个handler
        MyHandler handler = getHandler(req);
        if(handler == null){
            resp.getWriter().write("404 Page Not Found");
            return;
        }
        //再取一个适配器
        MyHandlerAdapter ha = getHandlerAdapter(handler);
        //再由适配器去调用具体的方法（真正调用的过程）
        MyModelAndView mv = ha.handle(req, resp, handler);
        
        //解析模板 @{name}
        applyDefaultViewName(resp, mv);
    }

    private void applyDefaultViewName(HttpServletResponse resp, MyModelAndView mv) throws IOException {
        if(null == mv) return;
        if(viewResolvers.isEmpty()) return;
        
        for (ViewResolver viewResolver : viewResolvers){
            if(!mv.getView().equals(viewResolver.getViewName())) continue;

            String parse = viewResolver.parse(mv);
            if(parse != null){
                resp.getWriter().write(parse);
                break;
            }
        }
    }

    private MyHandler getHandler(HttpServletRequest req){
        if(handlerMapping.isEmpty()) return null;
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath,"").replaceAll("/+","/");

        for (MyHandler handler: handlerMapping) {
            Matcher matcher = handler.pattern.matcher(url);
            if(!matcher.matches()) continue;
            return handler;
        }
        return null;
    }
    private MyHandlerAdapter getHandlerAdapter (MyHandler handler){
        if(adapterMapping.isEmpty()) return null;
        return adapterMapping.get(handler);
    }
    
    
    
    
    private void initMultipartResolver(MyApplicationContext context) {
    }

    private void initLocaleResolver(MyApplicationContext context) {
    }

    private void initThemeResolver(MyApplicationContext context) {
    }
    //解析url和method关系
    private void initHandlerMappings(MyApplicationContext context) {
        //找出所有加了controller注解的类
        //必须加了RequestMapping注解，才能被外界访问
        //再保存url=method的关系
        Map<String, Object> ioc = context.getAll();
        if(ioc.isEmpty()) return;
        for (Map.Entry<String,Object> entry: ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(MyController.class)) continue;
            String url = "";
            if(clazz.isAnnotationPresent(MyRequestMapping.class)){
                MyRequestMapping annotation = clazz.getAnnotation(MyRequestMapping.class);
                url = annotation.value();
            }
            Method[] methods = clazz.getMethods();
            for (Method method : methods){
                if(!method.isAnnotationPresent(MyRequestMapping.class)) continue;

                MyRequestMapping annotation = method.getAnnotation(MyRequestMapping.class);
                String regex = (url + annotation.value()).replaceAll("/+", "/");
                Pattern pattern = Pattern.compile(regex);
                handlerMapping.add(new MyHandler(entry.getValue(), method, pattern));
                
                System.out.println("Mapping: "+ regex);
                System.out.println("Controller: "+entry.getValue() + method);
            }
        }
    }

    //适配器，动态匹配参数
    private void initHandlerAdapters(MyApplicationContext context) {
        if(handlerMapping.isEmpty()) return;
        //参数号作为key,索引号作为值
        Map<String,Integer> paramMapping = new HashMap<>();
        for (MyHandler handler: handlerMapping){
            //匹配request response参数
            Class<?>[] parameterTypes = handler.method.getParameterTypes();
            for(int i=0 ; i < parameterTypes.length ; i++){
                Class<?> parameter = parameterTypes[i];
                if(parameter == HttpServletRequest.class || parameter == HttpServletResponse.class){
                    paramMapping.put(parameter.getName(),i);
                }
            }
            //匹配自定义参数，每个参数上可能有多个注解，所以这里是个二位数组
            Annotation[][] pa = handler.method.getParameterAnnotations();
            for(int i=0 ; i < pa.length ; i++) {
                for (Annotation a : pa[i]) {
                    if(a instanceof MyRequestParam){
                        String paramName = ((MyRequestParam) a).value();
                        if(!"".equals(paramName)){
                            paramMapping.put(paramName.trim(),i);
                        }
                    }
                }
            }

            adapterMapping.put(handler, new MyHandlerAdapter(paramMapping));
        }
    }

    private void initHandlerExceptionResolvers(MyApplicationContext context) {}

    private void initRequestToViewNameTranslator(MyApplicationContext context) {}

    private void initViewResolvers(MyApplicationContext context) {
        //模板通常放在WEB-INF或classes下
        String templateRoot = context.getProperties().getProperty("templateRoot");
        String rootPath = this.getClass().getClassLoader().getResource(templateRoot).getFile();
        File rootDir = new File(rootPath);
        for (File template : rootDir.listFiles()){
            viewResolvers.add(new ViewResolver(template.getName(), template));
        }
    }

    private void initFlashMapManager(MyApplicationContext context) {}

    /**
     * 方法适配器
     */
    private class MyHandlerAdapter{
        private Map<String,Integer> paramMapping;
        public MyHandlerAdapter(Map<String,Integer> paramMapping){
            this.paramMapping = paramMapping;
        }
        //主要目的是反射调用url对应的method
        public MyModelAndView handle(HttpServletRequest req, HttpServletResponse resp, MyHandler handler) throws IOException, InvocationTargetException, IllegalAccessException {
            //req resp也要赋值上去
            Class<?>[] paramTypes = handler.method.getParameterTypes();
            Object[] paramValues = new Object[paramTypes.length];
            Map<String,String[]> params = req.getParameterMap();
            for (Map.Entry<String, String[]> param : params.entrySet()) {
                String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
                if(!this.paramMapping.containsKey(param.getKey())) continue;
                int index = this.paramMapping.get(param.getKey());
                paramValues[index] = castParamValue(value,paramTypes[index]);
            }
            String reqName = HttpServletRequest.class.getName();
            if(paramMapping.containsKey(reqName)){
                int reqIndex = this.paramMapping.get(reqName);
                paramValues[reqIndex] = req;
            }
            String respName = HttpServletResponse.class.getName();
            if(paramMapping.containsKey(respName)) {
                int respIndex = this.paramMapping.get(respName);
                paramValues[respIndex] = resp;
            }
            boolean isMv = handler.method.getReturnType() == MyModelAndView.class;
            Object result = handler.method.invoke(handler.controller, paramValues);
            return isMv ? (MyModelAndView)result : null;
        }
        
        private Object castParamValue(String value,Class<?> clazz){
            if(clazz == String.class){
                return value;
            }else if(clazz == Integer.class){
                return Integer.valueOf(value);
            }else if(clazz == int.class){
                return Integer.valueOf(value).intValue();
            }
            return null;
        }
    }

    /**
     * handMapping定义
     */
    private class MyHandler{
        protected Object controller;
        protected Method method;
        protected Pattern pattern;
        
        protected MyHandler(Object controller,Method method,Pattern pattern){
            this.controller = controller;
            this.method = method;
            this.pattern = pattern;
        }
    }
    
    private class ViewResolver{
        protected String viewName;
        protected File file;

        protected ViewResolver(String viewName, File file) {
            this.viewName = viewName;
            this.file = file;
        }
        protected String parse(MyModelAndView mv) throws IOException {
            StringBuffer sb = new StringBuffer();
            //随机读写文件流  r  w  rw
            RandomAccessFile ra = new RandomAccessFile(this.file,"r");
            try {
                String line = null;
                while(null != (line=ra.readLine())){
                    Matcher m = matcher(line);
                    while(m.find()){
                        for (int i=1;i<=m.groupCount();i++){
                            String paramName = m.group(i);
                            Object paramValue = mv.getModel().get(paramName);
                            if(null == paramValue) continue;
                            line = line.replaceAll("@\\{"+paramName+"\\}",paramValue.toString());
                        }
                    }
                    sb.append(line);
                }
            }finally {
                ra.close();
            }
            return sb.toString();
        }
        
        private Matcher matcher(String str){
            Pattern pattern = Pattern.compile("@\\{(.+?)\\}",Pattern.CASE_INSENSITIVE);
            return pattern.matcher(str);
        }
        
        public String getViewName() {
            return viewName;
        }
    }
}
