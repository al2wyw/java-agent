package com.test;

import java.lang.instrument.Instrumentation;

/**
 * Created by IntelliJ IDEA.
 * User: win10
 * Date: 2019/4/14
 * Time: 9:57
 * Desc:
 */
public class Agent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("Agent.premain");
        Class[] klasss = inst.getAllLoadedClasses();
        for(int i = 0; i < klasss.length; i++){
            System.out.println(klasss[i].getName());
        }
        inst.addTransformer(new MaxLoopTransformer());
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("Agent.agentmain");
        Class[] klasss = inst.getAllLoadedClasses();
        for(int i = 0; i < klasss.length; i++){
            System.out.println(klasss[i].getName());
        }
        inst.addTransformer(new MaxLoopTransformer());
    }
}