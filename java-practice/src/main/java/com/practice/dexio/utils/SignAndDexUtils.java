package com.practice.dexio.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/2/2
 */
public class SignAndDexUtils {

    public static void sign(File unSignApkFile, File signApkFile) {
        String[] signCommand = {"cmd.exe", "/c ", "jarsigner", "-sigalg", "MD5withRSA",
                "-digestalg", "SHA1",
                "-keystore", "E:/OkhttpExperience/java-practice/src/main/java/com/practice/dexio/resource/debug.keystore",
                "-storepass", "android",
                "-keypass", "android",
                "-signedjar", signApkFile.getAbsolutePath(), unSignApkFile.getAbsolutePath(),
                "androiddebugkey"};

        try {
            executeCmdCommand(signCommand);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static File aarToDex(File aarSourceFile, File aarDexFile) {
        File unZipFileDir = aarDexFile.getParentFile();
        ZipUtils.unzipApk(aarSourceFile, unZipFileDir);

        File aarJarFile = Objects.requireNonNull(unZipFileDir.listFiles(
                (file, s) -> "classes.jar".equals(s)))[0];

        String[] dexCommand = {"cmd.exe /c dx --dex --output=" + aarDexFile.getAbsolutePath()
                + " " + aarJarFile.getAbsolutePath()};
        try {
            executeCmdCommand(dexCommand);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return aarDexFile;
    }

    private static void executeCmdCommand(String[] dexCommand) throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();

        StringBuffer stringBuffer=new StringBuffer();
        for (String commandItem:dexCommand){
            stringBuffer.append(commandItem);
            stringBuffer.append(" ");
        }
        System.out.println("exe command::"+stringBuffer.toString());

        Process command ;
        //注意指令执行流程，单条就调用一个传参的，多个命令组合的调用数组
        if(dexCommand.length==1){
            command= runtime.exec(dexCommand[0]);
        }else {
            command=runtime.exec(dexCommand);
        }

        int commandResult = command.waitFor();
        System.out.println("cmd command exe result::" + commandResult);

        if (command.exitValue() != 0) {
            //批处理命令失败
            InputStream errorStream = command.getErrorStream();
            byte[] buffer = new byte[1024];
            int len;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            while ((len = errorStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
            String result = new String(byteArrayOutputStream.toByteArray(),"GBK");
            byteArrayOutputStream.close();
            errorStream.close();
            System.out.println("execute command failed:" + result);
        }

        command.destroy();

    }
}
