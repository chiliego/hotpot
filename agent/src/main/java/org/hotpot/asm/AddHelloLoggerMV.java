package org.hotpot.asm;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ARRAYLENGTH;
import static org.objectweb.asm.Opcodes.ASM8;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.F_CHOP;
import static org.objectweb.asm.Opcodes.F_FULL;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class AddHelloLoggerMV extends MethodVisitor {

    private String owner;

    public AddHelloLoggerMV(String owner, MethodVisitor mv) {
        super(ASM8, mv);
        this.owner = owner;
    }

    @Override
    public void visitCode() {
        // org.chiliego.Timer
        systemOut("Hello ClassLoader!");
        systemOut("Class Name: " + getClassName());
        //loadTimerClass();
        // fromStub();
    }

    private void systemOut(String msg) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn(msg);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }

    private String getClassName() {
        Path path = Paths.get("/workspaces/hotpot/agent/bin/main/org/hotpot/asm/AddHelloLoggerMV.class");
        try {
            byte[] byteCode = Files.readAllBytes(path);
            return ASMUtils.getClassName(byteCode);
        } catch (IOException e) {
            return "Could not get class name";
        }
    }

    private void loadTimerClass() {
        // 
        //  if (className.equals("org.chiliego.Timer")) {
        //      Path path = Paths.get("/workspaces/myApp/app/bin/main/org/chiliego/Timer.class");
        //      try {
        //          byte[] b = Files.readAllBytes(path);
        //          return defineClass(className, b, 0, b.length);
        //      } catch (IOException e) {
        //          e.printStackTrace();
        //      }
        //  } 
        //

        Label label0 = new Label();
        Label label1 = new Label();
        Label label2 = new Label();
        mv.visitTryCatchBlock(label0, label1, label2, "java/io/IOException");
        Label label4 = new Label();
        mv.visitLabel(label4);
        mv.visitLineNumber(24, label4);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitLdcInsn("org.chiliego.Timers");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
        Label label5 = new Label();
        mv.visitJumpInsn(IFEQ, label5);
        Label label6 = new Label();
        mv.visitLabel(label6);
        mv.visitLineNumber(25, label6);
        mv.visitLdcInsn("/workspaces/myApp/app/bin/main/org/chiliego/Timers.class");
        mv.visitInsn(ICONST_0);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/String");
        mv.visitMethodInsn(INVOKESTATIC, "java/nio/file/Paths", "get",
                "(Ljava/lang/String;[Ljava/lang/String;)Ljava/nio/file/Path;", false);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitLabel(label0);
        mv.visitLineNumber(27, label0);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKESTATIC, "java/nio/file/Files", "readAllBytes", "(Ljava/nio/file/Path;)[B",
                false);
        mv.visitVarInsn(ASTORE, 3);
        Label label7 = new Label();
        mv.visitLabel(label7);
        mv.visitLineNumber(28, label7);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitMethodInsn(INVOKEVIRTUAL, owner, "defineClass",
                "(Ljava/lang/String;[BII)Ljava/lang/Class;", false);
        mv.visitLabel(label1);
        mv.visitInsn(ARETURN);
        mv.visitLabel(label2);
        mv.visitLineNumber(29, label2);
        mv.visitFrame(F_FULL, 3,
                new Object[] { owner, "java/lang/String", "java/nio/file/Path" }, 1,
                new Object[] { "java/io/IOException" });
        mv.visitVarInsn(ASTORE, 3);
        Label label8 = new Label();
        mv.visitLabel(label8);
        mv.visitLineNumber(31, label8);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/IOException", "printStackTrace", "()V", false);
        mv.visitLabel(label5);
        mv.visitLineNumber(34, label5);
        mv.visitFrame(F_CHOP, 1, null, 0, null);
    }
}
