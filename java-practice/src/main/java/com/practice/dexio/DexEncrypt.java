package com.practice.dexio;

import com.practice.dexio.utils.FileUtils;
import com.practice.dexio.utils.SignAndDexUtils;
import com.practice.dexio.utils.ZipUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Description:dex加密流程实现
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/2/1
 */
public class DexEncrypt {

    private static final String AES_PWD = "abcdefghijklmnop";
    private static final String resourcePath = "E:\\OkhttpExperience\\java-practice\\src\\main\\java\\com\\practice\\dexio\\resource";


    /**
     * 清除最近工作产出文件
     */
    private static void clearLatestWorkFile(boolean isDelResult) {

        String temptApkFilePath = resourcePath + "\\source\\apk\\tempt";
        FileUtils.deleteFileOrDir(temptApkFilePath, true);


        String temptAarFilePath = resourcePath + "\\source\\aar\\tempt";
        FileUtils.deleteFileOrDir(temptAarFilePath, true);

        if (isDelResult) {
            String resultFilePath = resourcePath + "\\result";
            FileUtils.deleteFileOrDir(resultFilePath, true);
        }
    }


    public static void main(String[] args) {
        //初始化aes
        AES.init(AES_PWD);
        //清除上次工作空间
        clearLatestWorkFile(true);
        //解压源apk文件并加密dex
        File dexDir = unZipSourceApkAndEncryptDex();

        //将aar jar转换成dex
        File hostDexFile = dexAar();
        //生成新的apk并签名
        generateApkAndSign(hostDexFile, dexDir);

        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(5));
            clearLatestWorkFile(false);
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
        }
    }

    private static File unZipSourceApkAndEncryptDex() {
        File srcApkFile = new File(resourcePath + "\\source\\apk\\source.apk");
        File destApkFile = new File(resourcePath + "\\source\\apk\\tempt");
        ZipUtils.unzipApk(srcApkFile, destApkFile);
        return AES.encryptApkDexFile(destApkFile);
    }


    private static File dexAar() {
        File aarSourceFile = new File(resourcePath + "\\source\\aar\\host.aar");
        File aarDexFile = new File(resourcePath + "\\source\\aar\\tempt\\classes.dex");
        return SignAndDexUtils.aarToDex(aarSourceFile, aarDexFile);
    }

    private static void generateApkAndSign(File hostDexFile, File dexFileDir) {

        try {
            //将hostDexFile合并到encryptDexFile目录下
            File moveFile = new File(dexFileDir, hostDexFile.getName());
            FileOutputStream fileOutputStream = new FileOutputStream(moveFile);
            fileOutputStream.write(FileUtils.getBytes(hostDexFile));
            fileOutputStream.flush();
            fileOutputStream.close();

            //生成未签名的apk
            File unSignApkFile = new File(resourcePath + "\\result\\un_sign.apk");

            ZipUtils.zip(dexFileDir, unSignApkFile);

            File signApkFile = new File(resourcePath + "\\result\\sign.apk");
            SignAndDexUtils.sign(unSignApkFile, signApkFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
