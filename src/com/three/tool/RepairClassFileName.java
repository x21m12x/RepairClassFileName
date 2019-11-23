package com.three.tool;

import java.io.*;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.util.LinkedList;
import java.util.List;

public class RepairClassFileName {


    public static void main(String[] args) {
        String strSrcClassFilesDir = args[0];
        String strDstClassFilesDir = args[1];

        Path srcPathDir = Paths.get(strSrcClassFilesDir);
        List<Path> fileList = new LinkedList<Path>();
        try {
            Files.walkFileTree(srcPathDir, new FindClassVisitor(fileList));
        } catch (IOException e) {
            e.printStackTrace();
        }


        MyLoader clsLoader = new MyLoader();
        for (Path classFile: fileList) {
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(classFile.toAbsolutePath().toString(), "r");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            FileChannel fc = raf.getChannel();

            try {
                MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, raf.length());
                String strClassRealName = clsLoader.GetClassName(mbb);
                WriteClassFile(strDstClassFilesDir, strClassRealName, mbb);
                raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void WriteClassFile(String strDstClassFilesDir, String strClassRealName, ByteBuffer mbb) throws IOException {
       Path clsPath = Paths.get(strDstClassFilesDir).resolve(strClassRealName.replace(".", File.separator) + ".class");
       if (!clsPath.toFile().getParentFile().exists()){
           if (!clsPath.toFile().getParentFile().mkdirs()){
               throw new IOException("文件夹创建失败：" +  clsPath.toFile().getParentFile().getAbsolutePath());
           }
       }
       FileOutputStream fileOut = new FileOutputStream(clsPath.toFile());
       while(mbb.hasRemaining()){
           fileOut.write(mbb.get());
       }
       fileOut.close();
    }

    private static class FindClassVisitor extends SimpleFileVisitor<Path> {
        private List<Path> result;

        public FindClassVisitor(List<Path> result) {
            this.result = result;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toString().endsWith(".class")) {
                result.add(file);
            }
            return FileVisitResult.CONTINUE;
        }
    }

    private static class MyLoader extends SecureClassLoader {
        String GetClassName(ByteBuffer bufClassBytes) {
            Class clazz = defineClass(null, bufClassBytes, (CodeSource) null);
            return clazz.getName();
        }
    }
}
