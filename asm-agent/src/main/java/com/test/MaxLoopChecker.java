package com.test;

/**
 * Created with IntelliJ IDEA.
 * User: liyang
 * Date: 2023-07-21
 * Time: 17:10
 * Description: 打包到agent.jar，可以被classloader加载，不需要额外触发
 */
public class MaxLoopChecker {
    private static final int MAX_LOOP = 1000;

    public static void checkLoop(int loop) {
        if (loop > MAX_LOOP) {
            throw new RuntimeException("loop exceeds max " + MAX_LOOP);
        }
    }
}
