package com.boot;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.Descriptor;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by IntelliJ IDEA.
 * User: win10
 * Date: 2019/4/14
 * Time: 16:15
 * Desc: similar to Main2
 */
public class Main3 {

    public static void main( String args[] ) throws Exception{
        URLClassLoader appLoader = (URLClassLoader)Thread.currentThread().getContextClassLoader();
        URL[] urls = appLoader.getURLs();
        TransformClassLoader loader = new TransformClassLoader(urls,null);
        Class klass = loader.loadClass("com.test.Invoker");
        Object invoker = klass.newInstance();
        Method method = klass.getDeclaredMethod("invoke",null);
        method.invoke(invoker,null);
    }

    private static class TransformClassLoader extends URLClassLoader{

        private ClassPool cp = ClassPool.getDefault();

        public TransformClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if(name.equals("com.test.Invoker")) {
                String path = name.replace('.', '/').concat(".class");
                try {
                    InputStream input = super.getResourceAsStream(path);

                    if (input == null) {
                        return super.findClass(name);
                    }
                    byte[] content = IOUtils.toByteArray(input);
                    IOUtils.closeQuietly(input);

                    ByteArrayInputStream bin = new ByteArrayInputStream(content);
                    CtClass ctClass = cp.makeClass(bin);
                    CtMethod ctMethod = ctClass.getMethod("invoke", Descriptor.ofMethod(CtClass.voidType, null));
                    ctMethod.insertBefore("System.out.println(\"agent invoke\");");

                    byte[] newContent = ctClass.toBytecode();
                    return defineClass(name, newContent, 0, newContent.length);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return super.findClass(name);
        }
    }
}