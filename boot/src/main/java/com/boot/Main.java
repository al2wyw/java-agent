package com.boot;

import com.test.Invoker;

/**
 * Created by IntelliJ IDEA.
 * User: win10
 * Date: 2019/4/14
 * Time: 11:24
 * Desc: -javaagent:agent-1.0.jar
 */
public class Main {

    public static void main( String args[] ) throws Exception{
        Invoker invoker = new Invoker();
        invoker.invoke();
    }
}