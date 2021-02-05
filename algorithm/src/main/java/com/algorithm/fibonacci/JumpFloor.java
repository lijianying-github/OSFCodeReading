package com.algorithm.fibonacci;

import java.util.concurrent.TimeUnit;

/**
 * Description:
 * <p>
 * 题目描述:
 * 一只青蛙一次可以跳上1级台阶，也可以跳上2级。求该青蛙跳上一个n级的台阶总共有多少种跳法（先后次序不同算不同的结果）。
 * </p>
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/2/5
 */
public class JumpFloor {

    //first 递归(2的次方)
    public static int jumpFloorByRecursion(int target) {

        if (target == 0 || target == 1 || target == 2) {
            return target;
        }
        return jumpFloorByRecursion(target - 1) + jumpFloorByRecursion(target - 2);
    }

    //second 记忆搜索法
    public static int jumpFloorBySearch(int target) {

        if (target <= 2) {
            return target;
        }

        //target-2台阶的次数，初始为target=1的结果
        int result_2 = 1;
        //target-1台阶的次数，初始为target=2的结果
        int result_1 = 2;
        //当前台阶次数
        int result = 0;

        for (int i = 3; i <= target; i++) {
            //计算当前
            result = result_1 + result_2;

            //前进一个台阶
            result_2 = result_1;
            result_1 = result;
        }

        return result;
    }

    public static void main(String[] args) {

        int target = 100;
        long start = System.currentTimeMillis();

        System.out.println("JumpFloor target::" + target
                + "  result::" + jumpFloorBySearch(target)
                + "  spendTime: " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start));
    }

}
