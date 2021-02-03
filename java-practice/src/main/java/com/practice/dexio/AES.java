package com.practice.dexio;

import android.annotation.SuppressLint;

import com.practice.dexio.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Description:注意密码长度必须16，24，32
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/2/1
 */
public class AES {

    private static final String algorithm = "AES/ECB/PKCS5Padding";


    private static Cipher encryptCipher;
    private static Cipher decryptCipher;


    @SuppressLint("GetInstance")
    public static void init(String aesPwd) {
        try {

            //加解密密钥
            SecretKeySpec key = new SecretKeySpec(aesPwd.getBytes(), "AES");

            //创建加解密类
            encryptCipher = Cipher.getInstance(algorithm);
            decryptCipher = Cipher.getInstance(algorithm);

            //初始化加解密
            encryptCipher.init(Cipher.ENCRYPT_MODE, key);
            decryptCipher.init(Cipher.DECRYPT_MODE, key);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    public static File encryptApkDexFile(File dexFile) {

        if (dexFile == null || !dexFile.exists()) {
            System.out.println("encrypt dexFile not exist");
            return null;
        }

        File[] dexFiles = dexFile.listFiles((file, fileName) -> fileName.endsWith(".dex"));
        if (dexFiles == null || dexFiles.length == 0) {
            return null;
        }
        File resultFile = null;

        for (File dex : dexFiles) {
            if (dex.getName().endsWith("classes.dex")) {
                //apk 主dex
                resultFile = dex.getParentFile();
            }

            byte[] dexSourceBytes = encrypt(FileUtils.getBytes(dex));
            if (dexSourceBytes == null || dexSourceBytes.length == 0) {
                continue;
            }

            try {
                //加密覆盖源文件
                File newFile=new File(dex.getAbsolutePath().replace(".dex","_.dex"));
                FileOutputStream outputStream = new FileOutputStream(newFile);
                outputStream.write(dexSourceBytes);
                outputStream.flush();
                outputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (File dex : dexFiles) {
           dex.delete();
        }

        return resultFile;
    }

    public static byte[] encrypt(byte[] content) {

        if (content == null || content.length == 0) {
            return null;
        }

        try {
            return encryptCipher.doFinal(content);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] decrypt(byte[] encryptContent) {

        if (encryptContent == null || encryptContent.length == 0) {
            return null;
        }
        try {
            return decryptCipher.doFinal(encryptContent);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }

}
