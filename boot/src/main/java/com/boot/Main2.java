package com.boot;

import com.test.Invoker;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: win10
 * Date: 2019/4/14
 * Time: 12:17
 * Desc:
 */
public class Main2 {

    public static void main( String args[] ) throws Exception{
        if(!Invoker.init) {
            URLClassLoader appLoader = (URLClassLoader)Thread.currentThread().getContextClassLoader();
            URL[] urls = appLoader.getURLs();
            MyClassLoader myClassLoader = new MyClassLoader(urls, null);
            Thread a = new Thread(() -> {
                try {
                    URLClassLoader myAppLoader = (URLClassLoader)Thread.currentThread().getContextClassLoader();
                    Class klass = myAppLoader.loadClass("com.boot.Main2");
                    Method method = klass.getMethod("main", String[].class);
                    method.invoke(null, new Object[]{null});

                }catch (Exception e){
                    e.printStackTrace();
                }
            });
            a.setContextClassLoader(myClassLoader);
            a.start();
            return;
        }
        Invoker invoker = new Invoker();
        invoker.invoke();
    }

    private static class MyClassLoader extends URLClassLoader{

        private static Map<String,Class> CLASS_CACHE = new HashMap<>();

        public MyClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
            try {
                URL url = new URL("file", "", "/D:/IdeaProjects/startup/agent-1.0.jar");//see Launcher getFileURL
                URLClassLoader myClassLoader = new URLClassLoader(new URL[]{url}, null);
                Class pre = myClassLoader.loadClass("com.test.Invoker");
                Field field = pre.getDeclaredField("init");
                field.setBoolean(null,true);
                CLASS_CACHE.put(pre.getName(),pre);
            }catch (Exception e){
                e.printStackTrace();
            }
        }


        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if(CLASS_CACHE.containsKey(name)){
                return CLASS_CACHE.get(name);
            }
            return super.findClass(name);
        }
    }
}