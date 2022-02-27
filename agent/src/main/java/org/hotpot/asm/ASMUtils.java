package org.hotpot.asm;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.util.TraceClassVisitor;

public final class ASMUtils {
    private static Logger LOGGER = LogManager.getLogger(ModMethodAdapter.class);

    private ASMUtils() {
    }

    public static void print(byte[] byteCode) {
        ClassReader cr = new ClassReader(byteCode);
        PrintWriter printWriter = new PrintWriter(System.out);
        TraceClassVisitor traceClassVisitor = new TraceClassVisitor(printWriter);
        cr.accept(traceClassVisitor, 0);
    }

    public static String getClassName(byte[] byteCode) {
        return getClassNameInternal(byteCode).replace("/", ".");
    }

    public static String getClassNameInternal(byte[] byteCode) {
        ClassReader classReader = new ClassReader(byteCode);
        ClassNameAdapter classNameAdapter = new ClassNameAdapter();
        classReader.accept(classNameAdapter, 0);
        return classNameAdapter.getClassName();
    }

    public static boolean hasReference(InputStream classIS, String toClass) {
        try (classIS) {
            ClassReader classReader = new ClassReader(classIS);
            ClassRefAdapter classRefAdapter = new ClassRefAdapter(toClass);
            classReader.accept(classRefAdapter, 0);
            return classRefAdapter.hasRef();
        } catch (IOException e) {
            //e.printStackTrace();
        }

        return false;
    }

    public static boolean hasReference(InputStream classIS, String classRef, String subClassRef) {
        try (classIS) {
            ClassReader classReader = new ClassReader(classIS);
            ClassRefAdapter classRefAdapter = new ClassRefAdapter(classRef);
            classReader.accept(classRefAdapter, 0);
            return classRefAdapter.hasRef();
        } catch (IOException e) {
            //e.printStackTrace();
        }

        return false;
    }

    public static byte[] redefineReference(String superName, String subName, InputStream classIS) {
        try {
            ClassReader classReader = new ClassReader(classIS);
            return redefineReference(superName, subName, classReader);
        } catch (IOException e) {
            // this means we can not load the class IS
        }

        return null;
    }

    public static byte[] redefineReference(String superName, String subName, byte[] b) {
        ClassReader classReader = new ClassReader(b);
        return redefineReference(superName, subName, classReader);
    }

    public static byte[] redefineReference(String superName, String subName, ClassReader classReader) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassRefAdapter classRefAdapter = new ClassRefAdapter(cw, superName, subName);
        classReader.accept(classRefAdapter, 0);

        if (classRefAdapter.hasRef()) {
            return cw.toByteArray();

        }

        return null;
    }

    public static List<String> getMethods(Class<?> clazz) throws IOException {
        InputStream classIS = clazz.getClassLoader()
                .getResourceAsStream(clazz.getName().replace(".", "/") + ".class");
        ClassReader classReader = new ClassReader(classIS);
        return getMethods(classReader);
    }

    public static List<String> getMethods(byte[] byteCode) {
        ClassReader classReader = new ClassReader(byteCode);
        return getMethods(classReader);
    }

    public static List<String> getMethods(ClassReader classReader) {
        MethodListAdapter methodListAdapter = new MethodListAdapter();
        classReader.accept(methodListAdapter, 0);
        return methodListAdapter.getMethodList();
    }

    public static boolean onlyChangeMethodBody(Class<?> origClass, byte[] modifiedBytecode) throws IOException {
        List<String> origMethods = getMethods(origClass);
        return onlyChangeMethodBody(origMethods, modifiedBytecode);
    }

    public static boolean onlyChangeMethodBody(byte[] origBytecode, byte[] modifiedBytecode) {
        List<String> origMethods = getMethods(origBytecode);
        return onlyChangeMethodBody(origMethods, modifiedBytecode);
    }

    public static boolean onlyChangeMethodBody(List<String> origMethods, byte[] modifiedBytecode) {
        List<String> modifiedMethods = getMethods(modifiedBytecode);

        Collections.sort(origMethods);
        Collections.sort(modifiedMethods);

        return origMethods.equals(modifiedMethods);
    }

    public static byte[] applyClassVisitor(InputStream byteCodeIS, UnaryOperator<ClassVisitor> cvFunc,
            boolean printByteCode) {
        try {
            ClassReader cr = new ClassReader(byteCodeIS);
            return applyClassVisitor(cr, cvFunc, printByteCode);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static byte[] applyClassVisitor(byte[] byteCode, UnaryOperator<ClassVisitor> cvFunc, boolean printByteCode) {
        ClassReader cr = new ClassReader(byteCode);
        return applyClassVisitor(cr, cvFunc, printByteCode);
    }

    public static byte[] applyClassVisitor(ClassReader cr, UnaryOperator<ClassVisitor> cvFunc, boolean printByteCode) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = cvFunc.apply(cw);
        cr.accept(cv, 0);
        byte[] resultByteCode = cw.toByteArray();
        if (printByteCode) {
            print(resultByteCode);
        }

        return resultByteCode;
    }

    public static String getMethodDescriptor(java.lang.reflect.Method method) {
        return Method.getMethod(method).getDescriptor();
    }
}
