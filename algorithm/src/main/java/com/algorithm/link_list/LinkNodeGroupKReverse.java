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

        ListNode header = buildNodeFromArray(test);
        printListNode(header);
        printListNode(reverseListNode(header, 3));


//        int[] result = kElementStackReverse(test, 3);
        int[] result = reverseArray(test, 4);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        for (int item : result) {
            stringBuilder.append(item).append(",");
        }
        stringBuilder.append("}");
        System.out.println("k reverse:" + stringBuilder.toString());
    }

    private static ListNode buildNodeFromArray(int[] array) {

        if (array == null || array.length == 0) {
            return null;
        }

        ListNode head = new ListNode(-1);

        for (int i = array.length - 1; i >= 0; i--) {
            ListNode node = new ListNode(array[i]);
            node.next = head.next;
            head.next = node;
        }

        return head.next;
    }

    private static void printListNode(ListNode head) {
        if (head == null) {
            System.out.println("empty list node==");
        } else if (head.next == null) {
            System.out.println("ListNode:" + head.value);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{");
            while (head != null) {
                stringBuilder.append(head.value).append(" ");
                head = head.next;
            }
            stringBuilder.append("}");
            System.out.println("ListNode:" + stringBuilder.toString());
        }
    }

    /**
     * 分段反转链表
     *
     * @param header 链表头
     * @param k      分组个数
     * @return 反转后的链表
     */
    private static ListNode reverseListNode(ListNode header, int k) {
        if (header == null || header.next == null || k <= 1) {
            return header;
        }

        ListNode result = new ListNode(-1);
        result.next = header;

        ListNode preNode = result;
        ListNode currentNode = header;
        ListNode tempt;

        int listSize = 0;

        while (header != null) {
            listSize++;
            header = header.next;
        }

        int segment = listSize / k;
        for (int currentSegment = 0; currentSegment < segment; currentSegment++) {

            for (int reverseTimes = 1; reverseTimes < k; reverseTimes++) {
                //注意顺序，temp开始，pre.next结束
                tempt = currentNode.next;
                currentNode.next = tempt.next;
                tempt.next = preNode.next;
                preNode.next = tempt;
            }
            preNode = currentNode;
            currentNode = currentNode.next;
        }
        return result.next;

    }

    //TODO
    private static int[] reverseArray(int[] array, int k) {
        if (array == null || array.length == 0 || k <= 1) {
            return array;
        }

        int preIndex = -1;
        int currentIndex = 0;
        int tempIndex;

        int segment = array.length / k;
        for (int currentSegment = 0; currentSegment < segment; currentSegment++) {

            for (int reverseTimes = 1; reverseTimes < k; reverseTimes++) {

                //temp=current.next
                tempIndex = currentIndex + 1;
                int temp = array[tempIndex];

                //current.next=tempt.next
                array[currentIndex + 1] = array[tempIndex + 1];
                //tempt.next=pre.next
                array[tempIndex + 1] = array[preIndex + 1];
                //pre.next=tempt
                array[preIndex + 1] = temp;
            }

            preIndex=currentIndex;
            currentIndex++;

        }

        return array;

    }

    /**
     * 通过栈完成反转
     *
     * @param source 源表
     * @param k      分组
     * @return 反转后结果
     */
    private static int[] reverseArrayByStack(int[] source, int k) {

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
    public static class ListNode {

        int value;

        ListNode next;

        public ListNode(int value) {
            this.value = value;
        }
    }

}
