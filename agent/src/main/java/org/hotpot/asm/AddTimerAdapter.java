package org.hotpot.asm;

import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ASM4;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.LADD;
import static org.objectweb.asm.Opcodes.LSUB;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;

import org.objectweb.asm.AnnotationVisitor;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class AddTimerAdapter extends ClassVisitor {

    private String owner;
    private boolean isInterface;

    public AddTimerAdapter(ClassVisitor cv) {
        super(ASM8, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        cv.visit(version, access, name, signature, superName, interfaces);
        owner = name;
        isInterface = (access & ACC_INTERFACE) != 0;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
            String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);

        if (!isInterface && mv != null && name.equals("name")) {
            // MethodVisitor mvOrig = cv.visitMethod(access, "_" + name, descriptor,
            // signature, exceptions);
            /* ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            mv = cw.visitMethod(access, name, descriptor, signature, exceptions); */
            mv = new ReplaceMethodAdapter(mv);
        }

        return mv;
    }

    public class ReplaceMethodAdapter extends MethodVisitor {

        private MethodVisitor targetMv;

        public ReplaceMethodAdapter(MethodVisitor mv) {
            super(ASM8);
            this.targetMv = mv;
        }

        @Override
        public void visitCode() {
            targetMv.visitCode();
            targetMv.visitTypeInsn(NEW, "org/chiliego/Timer");
            targetMv.visitInsn(DUP);
            targetMv.visitMethodInsn(INVOKESPECIAL, "org/chiliego/Timer", "<init>", "()V", false);
            targetMv.visitVarInsn(ASTORE, 2);
            targetMv.visitVarInsn(ALOAD, 2);
            targetMv.visitMethodInsn(INVOKEVIRTUAL, "org/chiliego/Timer", "start", "()V", false);
            targetMv.visitVarInsn(ALOAD, 2);
            targetMv.visitMethodInsn(INVOKEVIRTUAL, "org/chiliego/Timer", "stop", "()V", false);
            targetMv.visitFieldInsn(GETSTATIC, "org/chiliego/FaultyClass", "LOGGER", "Lorg/apache/logging/log4j/Logger;");
            targetMv.visitLdcInsn("Replace: {}");
            targetMv.visitVarInsn(ALOAD, 2);
            targetMv.visitMethodInsn(INVOKEVIRTUAL, "org/chiliego/Timer", "getValue", "()J", false);
            targetMv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            targetMv.visitMethodInsn(INVOKEINTERFACE, "org/apache/logging/log4j/Logger", "info",
                    "(Ljava/lang/String;Ljava/lang/Object;)V", true);
            targetMv.visitInsn(RETURN);
        }

        @Override
        public void visitEnd() {
            targetMv.visitEnd();
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return targetMv.visitAnnotation(desc, visible);
        }

        @Override
        public void visitParameter(String name, int access) {
            targetMv.visitParameter(name, access);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            targetMv.visitMaxs(maxStack, maxLocals);
        }
    }

    /**
     * AddLoggerMethodAdapter
     */
    public class AddTimerMethodAdapter extends MethodVisitor {

        public AddTimerMethodAdapter(MethodVisitor mv) {
            super(ASM8, mv);
        }

        /*
         * @Override
         * public void visitCode() {
         * 
         * mv.visitCode();
         * mv.visitTypeInsn(NEW, "org/chiliego/Timer");
         * mv.visitInsn(DUP);
         * mv.visitMethodInsn(INVOKESPECIAL, "org/chiliego/Timer", "<init>", "()V",
         * false);
         * mv.visitVarInsn(ASTORE, 2);
         * mv.visitVarInsn(ALOAD, 2);
         * mv.visitMethodInsn(INVOKEVIRTUAL, "org/chiliego/Timer", "start", "()V",
         * false);
         * 
         * }
         */

        @Override
        public void visitInsn(int opcode) {
            if ((opcode >= IRETURN && opcode <= RETURN) || opcode == ATHROW) {
                mv.visitTypeInsn(NEW, "org/chiliego/Timer");
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKESPECIAL, "org/chiliego/Timer", "<init>", "()V", false);
                mv.visitVarInsn(ASTORE, 2);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/chiliego/Timer", "start", "()V", false);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/chiliego/Timer", "stop", "()V", false);
                mv.visitFieldInsn(GETSTATIC, "org/chiliego/FaultyClass", "LOGGER", "Lorg/apache/logging/log4j/Logger;");
                mv.visitLdcInsn("Timer: {}");
                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/chiliego/Timer", "getValue", "()J", false);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                mv.visitMethodInsn(INVOKEINTERFACE, "org/apache/logging/log4j/Logger", "info",
                        "(Ljava/lang/String;Ljava/lang/Object;)V", true);
            }
            mv.visitInsn(opcode);
        }

        /*
         * @Override
         * public void visitMaxs(int maxStack, int maxLocals) {
         * mv.visitMaxs(maxStack + 8, 4);
         * }
         */

    }
}
