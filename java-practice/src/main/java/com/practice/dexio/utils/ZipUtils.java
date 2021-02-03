package com.practice.dexio.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/2/2
 */
public class ZipUtils {


    public static void zip(File sourceFile, File zip) {
        if (sourceFile == null || !sourceFile.exists()) {
            return;
        }

        try {
            CheckedOutputStream out = new CheckedOutputStream(new FileOutputStream(zip), new CRC32());
            ZipOutputStream zipOutputStream = new ZipOutputStream(out);

            if (sourceFile.isDirectory()) {
                compressDir(sourceFile, zipOutputStream, "");
            } else {
                compressFile(sourceFile, zipOutputStream, "");
            }

            zipOutputStream.flush();
            zipOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void compressDir(File dir, ZipOutputStream zos,
                                    String basePath) throws Exception {
        File[] files = dir.listFiles();
        if (files.length < 1) {
            ZipEntry entry = new ZipEntry(basePath + dir.getName() + "/");
            zos.putNextEntry(entry);
            zos.closeEntry();
        }
        for (File file : files) {
            if (file.isFile()) {
                compressFile(file, zos, basePath + dir.getName() + "/");
            }else {
                compressDir(file, zos, basePath + dir.getName() + "/");
            }
        }
    }

    private static void compressFile(File file, ZipOutputStream zos, String dir)
            throws Exception {

        String dirName = dir + file.getName();

        String[] dirNameNew = dirName.split("/");

        StringBuffer buffer = new StringBuffer();

        if (dirNameNew.length > 1) {
            for (int i = 1; i < dirNameNew.length; i++) {
                buffer.append("/");
                buffer.append(dirNameNew[i]);

            }
        } else {
            buffer.append("/");
        }

        ZipEntry entry = new ZipEntry(buffer.toString().substring(1));
        zos.putNextEntry(entry);
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        int count;
        byte data[] = new byte[1024];
        while ((count = bis.read(data, 0, 1024)) != -1) {
            zos.write(data, 0, count);
        }
        bis.close();
        zos.closeEntry();
    }


    public static void unzipApk(File zip, File destFile) {
        try {
            ZipFile zipFile = new ZipFile(zip);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry element = entries.nextElement();
                String name = element.getName();
                if (name.equals("META-INF/CERT.RSA") || name.equals("META-INF/CERT.SF") || name
                        .equals("META-INF/MANIFEST.MF")) {
                    continue;
                }

                if (!element.isDirectory()) {
                    File file = new File(destFile, name);
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    InputStream inputStream = zipFile.getInputStream(element);
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, len);
                    }
                    inputStream.close();
                    fileOutputStream.close();
                }
            }
            zipFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
