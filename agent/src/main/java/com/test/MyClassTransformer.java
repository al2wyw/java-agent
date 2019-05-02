package com.test;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.Descriptor;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * Created by IntelliJ IDEA.
 * User: win10
 * Date: 2019/4/13
 * Time: 19:37
 * Desc:
 */
public class MyClassTransformer implements ClassFileTransformer{

    private ClassPool cp = ClassPool.getDefault();

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if("com/test/Invoker".equals(className)) {
            System.out.println("start to transform original class");
            try {
                ByteArrayInputStream bin = new ByteArrayInputStream(classfileBuffer);
                CtClass ctClass = cp.makeClass(bin);
                CtMethod ctMethod = ctClass.getMethod("invoke",Descriptor.ofMethod(CtClass.voidType,null));
                ctMethod.insertBefore("System.out.println(\"agent invoke\");");

                return ctClass.toBytecode();
            }catch (Exception e){
                e.printStackTrace();
            }
            return classfileBuffer;
        }
        return classfileBuffer;
    }
}