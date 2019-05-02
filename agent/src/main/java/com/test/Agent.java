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
        inst.addTransformer(new MyClassTransformer());
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("Agent.agentmain");
        inst.addTransformer(new MyClassTransformer());
    }
}