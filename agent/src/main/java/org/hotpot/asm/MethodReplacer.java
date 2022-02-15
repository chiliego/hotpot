package org.hotpot.asm;

import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ASM8;

import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class MethodReplacer extends ClassVisitor{

    private Predicate<String> methodName;
    private BiFunction<String, MethodVisitor, MethodVisitor> targetMv;
    private String owner;
    private boolean isInterface;

    public MethodReplacer(ClassVisitor classVisitor, Predicate<String> methodName, BiFunction<String, MethodVisitor, MethodVisitor> mv) {
        super(ASM8, classVisitor);
        this.methodName = methodName;
        this.targetMv = mv;
    }
    
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        cv.visit(version, access, name, signature, superName, interfaces);
        this.owner = name;
        this.isInterface = (access & ACC_INTERFACE) != 0;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
            String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);

        if (!isInterface && mv != null && methodName.test(name)) {
            mv = this.targetMv.apply(owner, mv);
        }

        return mv;
    }
}
