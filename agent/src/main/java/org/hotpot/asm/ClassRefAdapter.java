package org.hotpot.asm;

import static org.objectweb.asm.Opcodes.ASM8;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class ClassRefAdapter extends ClassVisitor {
    private static Logger LOGGER = LogManager.getLogger(ClassRefAdapter.class);

    private String superName;
    private String subName;
    private boolean hasRef = false;
    private String className;

    public ClassRefAdapter(String superName) {
        super(ASM8);
        this.superName = replaceDots(superName);
        this.subName = null;
    }

    public ClassRefAdapter(ClassVisitor cv, String superName, String subName) {
        super(ASM8, cv);
        this.superName = replaceDots(superName);
        this.subName = replaceDots(subName);
    }

    private String replaceDots(String name) {
        return name.replace(".", "/");
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }
    
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
            String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new MethodAdapter(mv);
    }

    private class MethodAdapter extends MethodVisitor {

        public MethodAdapter(MethodVisitor mv) {
            super(ASM8, mv);
        }
    
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            if (owner.equals(superName) && (opcode == INVOKESPECIAL || opcode == INVOKEVIRTUAL)) {
                LOGGER.info("visitMethodInsn {} - {} : {} : {}", className, owner, name, descriptor);
                if (subName != null) {
                    owner = subName;
                }
                hasRef = true;
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
        
        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (type.equals(superName) && opcode == NEW) {
                if (subName != null) {
                    type = subName;
                }
                hasRef = true;
            }
            super.visitTypeInsn(opcode, type);
        }
    }

    public boolean hasRef() {
        return hasRef;
    }
}
