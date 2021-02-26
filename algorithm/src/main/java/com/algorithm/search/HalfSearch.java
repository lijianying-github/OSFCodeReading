package com.algorithm.search;

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/2/26
 */
public class HalfSearch {

    /**
     * 二分查找
     * <p>
     *
     * 请实现有重复数字的升序数组的二分查找。
     * 输出在数组中第一个大于等于查找值的位置，如果数组中不存在这样的数，则输出数组长度加一。
     * </p>
     *
     * @param length int整型 数组长度
     * @param v int整型 查找值
     * @param array int整型一维数组 有序数组
     * @return int整型
     */
    public int upper_bound_ (int length, int v, int[] array) {

        if (v<=array[0]){
            //第一个大于等于v的就是第一个元素
            return 1;
        }

        //没有比指定值大的元素，返回未找到
        if(v>array[length-1]){
            return length+1;
        }

        int start=0;
        int end=length-1;
        int mid=(start+end)/2;

        while(start<=end){
            int midValue=array[mid];
            if(midValue>v){
                //v在左区间
                end=mid-1;
                mid=(start+end)/2;
            }else if(midValue<v){
                //v在右区间
                start=mid+1;
                mid=(start+end)/2;
            }else{
                //相等
                if(array[mid-1]!=v){
                    //最终命中
                    return mid+1;
                }else{
                    //存在重复多个，左区间减一和midValue>v一样
                    end=mid-1;
                    mid=(start+end)/2;
                }
            }
        }

        //没有找到
        return length+1;

    }

    public static void test_upper_bound(){
        HalfSearch halfSearch=new HalfSearch();
        //input :100,1,[2,3,4,4,4,7,7,8,10,10,11,12,13,14,15,15,17,18,19,23,24,24,24,24,25,26,26,26,27,27,28,29,29,30,33,36,38,38,40,40,41,43,43,43,44,46,46,47,51,52,52,53,54,56,57,57,57,58,58,61,61,61,62,64,64,66,66,67,67,67,70,72,74,74,74,75,75,78,78,78,79,79,80,83,83,83,83,84,84,86,88,89,89,90,91,91,92,93,93,96]
        // expect output:1
        int result=halfSearch.upper_bound_(100,1,new int[]{2,3,4,4,4,7,7,8,10,10,11,12,13,14,15,15,17,18,19,23,24,24,24,24,25,26,26,26,27,27,28,29,29,30,33,36,38,38,40,40,41,43,43,43,44,46,46,47,51,52,52,53,54,56,57,57,57,58,58,61,61,61,62,64,64,66,66,67,67,67,70,72,74,74,74,75,75,78,78,78,79,79,80,83,83,83,83,84,84,86,88,89,89,90,91,91,92,93,93,96});
        //out put
        System.out.println("upper_bound_result:: "+result);
    }

    public static void main(String[] args) {
        test_upper_bound();
    }

}
