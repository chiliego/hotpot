package org.hotpot.lang;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StubClassLoader extends ClassLoader {
    private static Logger LOGGER = LogManager.getLogger(StubClassLoader.class);
    private List<Path> classPaths;

    public StubClassLoader() {
        this.classPaths = new ArrayList<>();
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        System.out.println("Start IF");
        if (name.equals("org.chiliego.Timer")) {
            Path path = Paths.get("/workspaces/myApp/app/bin/main/org/chiliego/Timer.class");
            try {
                byte[] b = Files.readAllBytes(path);
                return defineClass(name, b, 0, b.length);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        System.out.println("End IF");
        return super.loadClass(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String findClassPath = name.replace(".", "/");
        ClassNotFoundException exp;

        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            exp = e;
        }

        for (Path path : classPaths) {
            Path absFindClassPath = path.resolve(findClassPath);

            if (Files.exists(absFindClassPath)) {
                try {
                    byte[] byteCode = Files.readAllBytes(absFindClassPath);
                    defineClass(name, byteCode, 0, byteCode.length);
                } catch (IOException e) {
                    LOGGER.error("Could not read class file " + absFindClassPath + ".", e);
                }
            }
        }

        throw exp;
    }

    /* @Override
    protected Class findClass(String name) throws ClassNotFoundException {
        if (name.endsWith("_Stub")) {
            ClassWriter cw = new ClassWriter(0);
            // ...
            byte[] b = cw.toByteArray();
            return defineClass(name, b, 0, b.length);
        }
        return super.findClass(name);
    } */

    public void addClassPaths(List<Path> classPaths) {
        classPaths.addAll(classPaths);
    }
}
