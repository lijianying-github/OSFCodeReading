package com.algorithm.tree;

/**
 * Description:二叉树对称判断
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/2/5
 */
public class SymmetricTree {

    public static boolean isSymmetric(TreeNode root) {

        if (root == null) {
            return true;
        }

        return isSymmetric(root.left, root.right);
    }

    public static boolean isSymmetric(TreeNode left, TreeNode right) {

        if (left == null && right == null) {
            return true;
        }

        if (left == null || right == null) {
            return false;
        }

        return left.val == right.val && isSymmetric(left.left, right.right) && isSymmetric(left.right, right.left);
    }

    public static void main(String[] args) {

        int level = 2;
        int[] treePreArray = {1, 2, 2};

        //构建二叉树
        TreeNode treeNode = new TreeNode(treePreArray[0]);
        treeNode.left = new TreeNode(treePreArray[1]);
        treeNode.right = new TreeNode(treePreArray[2]);

        //遍历二叉树
        System.out.println("isSymmetric::" + isSymmetric(treeNode));
    }

}
