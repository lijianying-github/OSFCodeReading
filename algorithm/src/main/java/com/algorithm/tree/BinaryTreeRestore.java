package com.algorithm.tree;

import java.util.Arrays;

/**
 * Description:二叉树重建
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/2/4
 */
public class BinaryTreeRestore {

    public static TreeNode reConstructBinaryTree(int[] pre, int[] in) {
        if (pre == null || pre.length == 0) {
            return null;
        }

        TreeNode root = new TreeNode(pre[0]);
        if (pre.length == 1) {
            return root;
        }

        for (int index = 0; index < in.length; index++) {
            if (in[index] == root.val) {
                root.left = reConstructBinaryTree(Arrays.copyOfRange(pre, 1, index + 1), Arrays.copyOfRange(in, 0, index));
                root.right = reConstructBinaryTree(Arrays.copyOfRange(pre, index + 1, pre.length), Arrays.copyOfRange(in, index + 1, in.length));
                break;
            }
        }

        return root;
    }

    public static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;

        TreeNode(int x) {
            val = x;
        }
    }

    public static void main(String[] args) {
        int[] pre = {1, 2, 4, 7, 3, 5, 6, 8};
        int[] in = {4, 7, 2, 1, 5, 3, 8, 6};

        TreeNode root = reConstructBinaryTree(pre, in);
        printTreeNode(root);

    }

    public static void printTreeNode(TreeNode node) {
        if (node == null) {
            System.out.println("treeNode null==");
            return;
        }

        if (node.left != null) {
            printTreeNode(node.left);
        }

        System.out.println("tree node :" + node.val);

        if (node.right != null) {
            printTreeNode(node.right);
        }
    }
}
