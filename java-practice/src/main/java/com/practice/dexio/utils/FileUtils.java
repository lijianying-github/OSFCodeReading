package com.practice.dexio.utils;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/2/1
 */
public class FileUtils {


    /**
     * 删除指定路径文件
     *
     * @param filePath       指定路径
     * @param notExistCreate 不存在是否创建
     */
    public static void deleteFileOrDir(String filePath, boolean notExistCreate) {
        try {
            File file = new File(filePath);

            if (file.exists()) {
                if (file.isDirectory()) {
                    //获取子文件
                    File[] files = file.listFiles();
                    for (File item : files) {
                        //只要是文件和空文件夹才可以删除
                        boolean isSuccess = item.delete();
                        if (isSuccess) {
                            //删除子空文件夹和文件成功
                        } else {
                            //删除失败，表明是文件夹且非空，递归删除子文件夹
                            deleteFileOrDir(item.getAbsolutePath(), false);
                            //删除空文件夹
                            item.delete();
                        }
                    }
                }
            } else {
                if (notExistCreate) {
                    file.mkdirs();
                }
            }
        } catch (Exception e) {
            System.out.println("delete file failed ==" + e.getMessage());
        }

    }

    public static byte[] getBytes(File file) {

        if (file == null || !file.exists()) {
            return null;
        }

        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            byte[] result = new byte[(int) file.length()];
            randomAccessFile.readFully(result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
