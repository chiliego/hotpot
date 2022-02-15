package org.hotpot.asm;

import java.io.PrintWriter;
import java.util.function.UnaryOperator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
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
}
