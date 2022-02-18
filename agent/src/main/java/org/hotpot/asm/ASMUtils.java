package org.hotpot.asm;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.util.TraceClassVisitor;

public final class ASMUtils {
    private ASMUtils(){
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
        try {
            ClassReader classReader = new ClassReader(classIS);
            ClassRefAdapter classRefAdapter = new ClassRefAdapter(toClass);
            classReader.accept(classRefAdapter, 0);
            return classRefAdapter.hasRef();
        } catch (IOException e) {
            //e.printStackTrace();
        }

        return false;
    }

    public static List<String> getMethods(byte[] byteCode) {
        ClassReader classReader = new ClassReader(byteCode);
        MethodListAdapter methodListAdapter = new MethodListAdapter();
        classReader.accept(methodListAdapter, 0);
        return methodListAdapter.getMethodList();
    }

    public static boolean onlyChangeMethodBody(byte[] origBytecode, byte[] modifiedBytecode) {
        List<String> origMethods = getMethods(origBytecode);
        List<String> modifiedMethods = getMethods(modifiedBytecode);

        Collections.sort(origMethods);
        Collections.sort(modifiedMethods);

        return origMethods.equals(modifiedMethods);
    }

    public static byte[] applyClassVisitor(byte[] byteCode, UnaryOperator<ClassVisitor> cvFunc, boolean printByteCode) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = cvFunc.apply(cw);
        ClassReader cr = new ClassReader(byteCode);
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
