package com.test;

import javassist.*;
import javassist.bytecode.Descriptor;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * Created by IntelliJ IDEA.
 * User: johnny.ly
 * Date: 2019/5/12
 * Time: 23:19
 * Desc:
 */
public class CleanerTransformer implements ClassFileTransformer {

    private ClassPool cp = ClassPool.getDefault();

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        String name = className.replace("/",".");

        if(name.equals("sun.misc.Cleaner")) {
            System.out.println("start to transform sun.misc.Cleaner class");
            try {
                ByteArrayInputStream bin = new ByteArrayInputStream(classfileBuffer);

                CtClass ctClass = cp.makeClass(bin);
                ctClass.addField(CtField.make("private String name;", ctClass));

                CtConstructor ctConstructor = ctClass.getDeclaredConstructor(new CtClass[]{cp.get("java.lang.Object"), cp.get("java.lang.Runnable")});
                ctConstructor.insertBeforeBody("this.name = utils.ThreadUtils.name.get();");

                CtMethod ctMethod = ctClass.getMethod("clean", Descriptor.ofMethod(CtClass.voidType, null));
                ctMethod.insertBefore("System.out.println(\"clean invoke: \" + this.name);");

                byte[] newContent = ctClass.toBytecode();

                ctClass.defrost();
                return newContent;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return classfileBuffer;
    }
}
