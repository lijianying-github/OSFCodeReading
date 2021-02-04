package com.algorithm.link_list;

/**
 * Description:链表中节点每K个一组翻转
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/2/4
 */
public class LinkNodeGroupKReverse {

    public static void main(String[] args) {
        int[] test = {1, 2, 3, 4, 5};
        int[] result = kElementStackReverse(test, 3);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        for (int item : result) {
            stringBuilder.append(item).append(",");
        }
        stringBuilder.append("}");
        System.out.println("k reverse:" + stringBuilder.toString());
    }


    /**
     * 通过栈完成反转
     *
     * @param source 源表
     * @param k      分组
     * @return 反转后结果
     */
    private static int[] kElementStackReverse(int[] source, int k) {

        if (source.length == 0) {
            return source;
        }

        int reverseRange = source.length / k * k;
        if (reverseRange == 0) {
            return source;
        }

        int[] result = new int[source.length];
        int[] stack = new int[k];

        int seg = 0;
        for (int i = 0; i < reverseRange; i++) {
            int index = (i + 1) % k;
            stack[k - index - 1] = source[i];

            if (index == 0) {
                //出栈
                for (int j = 0; j < k; j++) {
                    result[seg * k + j] = stack[k - 1 - j];
                }
                seg++;
            }
        }

        for (int i = seg * k; i < source.length; i++) {
            result[i] = source[i];
        }

        return result;
    }


    /**
     * 单链表节点定义
     */
    public static class LinkNode {

        int value;

        LinkNode next;

        public LinkNode(int value) {
            this.value = value;
        }
    }

}
