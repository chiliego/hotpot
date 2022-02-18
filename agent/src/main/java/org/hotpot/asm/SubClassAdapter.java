package org.hotpot.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class SubClassAdapter extends ClassVisitor {

    private String newClassName;
    private String newSuperName;
    private String oldSuperName;

    public SubClassAdapter(ClassVisitor cv, String newClassName) {
        super(Opcodes.ASM8, cv);
        this.newClassName = newClassName;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.newSuperName = name;
        oldSuperName = superName;

        super.visit(version, access, newClassName, signature, newSuperName, interfaces);
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(null, debug);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
            String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
        return new MethodAdapter(mv, name.equals("<init>"));
    }

    private class MethodAdapter extends MethodVisitor {
        private boolean isConstructor;

        public MethodAdapter(MethodVisitor mv, boolean isConstructor) {
            super(Opcodes.ASM8, mv);
            this.isConstructor = isConstructor;
        }

        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end,
                int index) {
            if (name.equals("this")) {
                descriptor = descriptor.replace(newSuperName, newClassName);
            }

            super.visitLocalVariable(name, descriptor, signature, start, end, index);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            if (opcode == Opcodes.INVOKESPECIAL && owner.equals(oldSuperName) && isConstructor && name.equals("<init>")) {
                owner = newSuperName;
            } 

            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }
}
