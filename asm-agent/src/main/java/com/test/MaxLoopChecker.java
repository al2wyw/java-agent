package com.test;

/**
 * Created with IntelliJ IDEA.
 * User: liyang
 * Date: 2023-07-21
 * Time: 17:10
 * Description:
 */
public class MaxLoopChecker {
    private static final int MAX_LOOP = 1000;

    public static void checkLoop(int loop) {
        if (loop > MAX_LOOP) {
            throw new RuntimeException("loop exceeds max " + MAX_LOOP);
        }
    }
}
