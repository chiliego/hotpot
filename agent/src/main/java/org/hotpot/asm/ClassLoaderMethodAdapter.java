package org.hotpot.asm;

import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ARRAYLENGTH;
import static org.objectweb.asm.Opcodes.ASM8;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.F_CHOP;
import static org.objectweb.asm.Opcodes.F_FULL;
import static org.objectweb.asm.Opcodes.F_SAME;
import static org.objectweb.asm.Opcodes.F_SAME1;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.IF_ACMPEQ;
import static org.objectweb.asm.Opcodes.INTEGER;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.MONITORENTER;
import static org.objectweb.asm.Opcodes.MONITOREXIT;
import static org.objectweb.asm.Opcodes.NEW;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class ClassLoaderMethodAdapter extends MethodVisitor {
    private String owner;
    private String pathToConfFile;

    public ClassLoaderMethodAdapter(String owner, MethodVisitor mv, String classPathConfFile) {
        super(ASM8, mv);
        this.owner = owner;
        this.pathToConfFile = classPathConfFile;
    }

    @Override
    public void visitCode() {
        modifyLoadClass_String_boolean();
    }

    /**
     * Add to begin of loadClass(String name, boolean resolve)
     * 
     * <pre>
     * synchronized (getClassLoadingLock(name)) {
     *      // First, check if the class has already been loaded
     *      Class<?> c = findLoadedClass(name);
     *      if (c == null) {
     *          // Class not loaded, try loading from our classpath
     *          Path classPathConf = Paths.get("/path/to/confFile");
     *          String classFile = name.replace(".", "/").concat(".class");
     * 
     *          try (BufferedReader br = Files.newBufferedReader(classPathConf)) {
     *              String classPathStr = br.readLine();
     *
     *              while (classPathStr != null) {
     *                  Path classPath = Paths.get(classPathStr);
     *                  Path classFilePath = classPath.resolve(classFile);
     *                  if (Files.exists(classFilePath)) {
     *                      byte[] b = Files.readAllBytes(classFilePath);
     *                      System.out.println("HotPot: stubbed loadClas(String, boolean) in " + this.getClass());
     *                      System.out.println("HotPot: define class from " + classFilePath.toAbsolutePath());
     *                      return defineClass(name, b, 0, b.length);
     *                  }
     *                  classPathStr = br.readLine();
     *              }
     *          } catch (IOException e) {
     *              System.out.println("HotPot error: stubbed loadClas(String, boolean) in " + this.getClass());
     *              System.out.println("HotPot error: could not define " + classFile);
     *              e.printStackTrace();
     *          }
     *      }
     *  }
     * </pre>
     */
    private void modifyLoadClass_String_boolean() {
        Label label0 = new Label();
        Label label1 = new Label();
        Label label2 = new Label();
        mv.visitTryCatchBlock(label0, label1, label2, null);
        Label label3 = new Label();
        Label label4 = new Label();
        mv.visitTryCatchBlock(label3, label4, label2, null);
        Label label5 = new Label();
        Label label6 = new Label();
        Label label7 = new Label();
        mv.visitTryCatchBlock(label5, label6, label7, null);
        mv.visitTryCatchBlock(label3, label7, label7, null);
        Label label8 = new Label();
        Label label9 = new Label();
        mv.visitTryCatchBlock(label8, label6, label9, "java/io/IOException");
        mv.visitTryCatchBlock(label3, label9, label9, "java/io/IOException");
        Label label10 = new Label();
        Label label11 = new Label();
        Label label12 = new Label();
        mv.visitTryCatchBlock(label10, label11, label12, null);
        Label label13 = new Label();
        mv.visitTryCatchBlock(label3, label13, label12, null);
        Label label14 = new Label();
        mv.visitTryCatchBlock(label12, label14, label12, null);
        Label label16 = new Label();
        mv.visitLabel(label16);
        mv.visitLineNumber(13, label16);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, owner, "getClassLoadingLock",
                "(Ljava/lang/String;)Ljava/lang/Object;", false);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitInsn(MONITORENTER);
        mv.visitLabel(label10);
        mv.visitLineNumber(15, label10);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, owner, "findLoadedClass", "(Ljava/lang/String;)Ljava/lang/Class;",
                false);
        mv.visitVarInsn(ASTORE, 4);
        Label label17 = new Label();
        mv.visitLabel(label17);
        mv.visitLineNumber(16, label17);
        mv.visitVarInsn(ALOAD, 4);
        Label label18 = new Label();
        mv.visitJumpInsn(IFNONNULL, label18);
        Label label19 = new Label();
        mv.visitLabel(label19);
        mv.visitLineNumber(18, label19);
        mv.visitLdcInsn(pathToConfFile);
        mv.visitInsn(ICONST_0);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/String");
        mv.visitMethodInsn(INVOKESTATIC, "java/nio/file/Paths", "get",
                "(Ljava/lang/String;[Ljava/lang/String;)Ljava/nio/file/Path;", false);
        mv.visitVarInsn(ASTORE, 5);
        Label label20 = new Label();
        mv.visitLabel(label20);
        mv.visitLineNumber(19, label20);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitLdcInsn(".");
        mv.visitLdcInsn("/");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "replace",
                "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;", false);
        mv.visitLdcInsn(".class");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "concat",
                "(Ljava/lang/String;)Ljava/lang/String;", false);
        mv.visitVarInsn(ASTORE, 6);
        mv.visitLabel(label8);
        mv.visitLineNumber(21, label8);
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ASTORE, 7);
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ASTORE, 8);
        mv.visitLabel(label5);
        mv.visitVarInsn(ALOAD, 5);
        mv.visitMethodInsn(INVOKESTATIC, "java/nio/file/Files", "newBufferedReader",
                "(Ljava/nio/file/Path;)Ljava/io/BufferedReader;", false);
        mv.visitVarInsn(ASTORE, 9);
        mv.visitLabel(label0);
        mv.visitLineNumber(22, label0);
        mv.visitVarInsn(ALOAD, 9);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/BufferedReader", "readLine", "()Ljava/lang/String;", false);
        mv.visitVarInsn(ASTORE, 10);
        Label label21 = new Label();
        mv.visitLabel(label21);
        mv.visitLineNumber(24, label21);
        Label label22 = new Label();
        mv.visitJumpInsn(GOTO, label22);
        Label label23 = new Label();
        mv.visitLabel(label23);
        mv.visitLineNumber(25, label23);
        mv.visitFrame(F_FULL, 11, new Object[] { owner, "java/lang/String", INTEGER, "java/lang/Object",
                "java/lang/Class", "java/nio/file/Path", "java/lang/String", "java/lang/Throwable",
                "java/lang/Throwable", "java/io/BufferedReader", "java/lang/String" }, 0,
                new Object[] {});
        mv.visitVarInsn(ALOAD, 10);
        mv.visitInsn(ICONST_0);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/String");
        mv.visitMethodInsn(INVOKESTATIC, "java/nio/file/Paths", "get",
                "(Ljava/lang/String;[Ljava/lang/String;)Ljava/nio/file/Path;", false);
        mv.visitVarInsn(ASTORE, 11);
        Label label24 = new Label();
        mv.visitLabel(label24);
        mv.visitLineNumber(26, label24);
        mv.visitVarInsn(ALOAD, 11);
        mv.visitVarInsn(ALOAD, 6);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/nio/file/Path", "resolve",
                "(Ljava/lang/String;)Ljava/nio/file/Path;", true);
        mv.visitVarInsn(ASTORE, 12);
        Label label25 = new Label();
        mv.visitLabel(label25);
        mv.visitLineNumber(27, label25);
        mv.visitVarInsn(ALOAD, 12);
        mv.visitInsn(ICONST_0);
        mv.visitTypeInsn(ANEWARRAY, "java/nio/file/LinkOption");
        mv.visitMethodInsn(INVOKESTATIC, "java/nio/file/Files", "exists",
                "(Ljava/nio/file/Path;[Ljava/nio/file/LinkOption;)Z", false);
        mv.visitJumpInsn(IFEQ, label3);
        Label label26 = new Label();
        mv.visitLabel(label26);
        mv.visitLineNumber(28, label26);
        mv.visitVarInsn(ALOAD, 12);
        mv.visitMethodInsn(INVOKESTATIC, "java/nio/file/Files", "readAllBytes", "(Ljava/nio/file/Path;)[B",
                false);
        mv.visitVarInsn(ASTORE, 13);
        Label label27 = new Label();
        mv.visitLabel(label27);
        mv.visitLineNumber(29, label27);
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("HotPot: stubbed loadClas(String, boolean) in ");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        Label label28 = new Label();
        mv.visitLabel(label28);
        mv.visitLineNumber(30, label28);
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("HotPot: define class from ");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitVarInsn(ALOAD, 12);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/nio/file/Path", "toAbsolutePath", "()Ljava/nio/file/Path;",
                true);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        Label label29 = new Label();
        mv.visitLabel(label29);
        mv.visitLineNumber(31, label29);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 13);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ALOAD, 13);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitMethodInsn(INVOKEVIRTUAL, owner, "defineClass", "(Ljava/lang/String;[BII)Ljava/lang/Class;",
                false);
        mv.visitLabel(label1);
        mv.visitLineNumber(35, label1);
        mv.visitVarInsn(ALOAD, 9);
        mv.visitJumpInsn(IFNULL, label6);
        mv.visitVarInsn(ALOAD, 9);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/BufferedReader", "close", "()V", false);
        mv.visitLabel(label6);
        mv.visitLineNumber(31, label6);
        mv.visitFrame(F_FULL, 14,
                new Object[] { owner, "java/lang/String", INTEGER, "java/lang/Object",
                        "java/lang/Class", "java/nio/file/Path", "java/lang/String",
                        "java/lang/Throwable", "java/lang/Throwable", "java/io/BufferedReader",
                        "java/lang/String", "java/nio/file/Path", "java/nio/file/Path", "[B" },
                1, new Object[] { "java/lang/Class" });
        mv.visitVarInsn(ALOAD, 3);
        mv.visitInsn(MONITOREXIT);
        mv.visitLabel(label11);
        mv.visitInsn(ARETURN);
        mv.visitLabel(label3);
        mv.visitLineNumber(33, label3);
        mv.visitFrame(F_CHOP, 1, null, 0, null);
        mv.visitVarInsn(ALOAD, 9);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/BufferedReader", "readLine", "()Ljava/lang/String;", false);
        mv.visitVarInsn(ASTORE, 10);
        mv.visitLabel(label22);
        mv.visitLineNumber(24, label22);
        mv.visitFrame(F_CHOP, 2, null, 0, null);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitJumpInsn(IFNONNULL, label23);
        mv.visitLabel(label4);
        mv.visitLineNumber(35, label4);
        mv.visitVarInsn(ALOAD, 9);
        mv.visitJumpInsn(IFNULL, label18);
        mv.visitVarInsn(ALOAD, 9);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/BufferedReader", "close", "()V", false);
        mv.visitJumpInsn(GOTO, label18);
        mv.visitLabel(label2);
        mv.visitFrame(F_FULL, 10, new Object[] { owner, "java/lang/String", INTEGER, "java/lang/Object",
                "java/lang/Class", "java/nio/file/Path", "java/lang/String", "java/lang/Throwable",
                "java/lang/Throwable", "java/io/BufferedReader" }, 1,
                new Object[] { "java/lang/Throwable" });
        mv.visitVarInsn(ASTORE, 7);
        mv.visitVarInsn(ALOAD, 9);
        Label label30 = new Label();
        mv.visitJumpInsn(IFNULL, label30);
        mv.visitVarInsn(ALOAD, 9);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/BufferedReader", "close", "()V", false);
        mv.visitLabel(label30);
        mv.visitFrame(F_CHOP, 1, null, 0, null);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitInsn(ATHROW);
        mv.visitLabel(label7);
        mv.visitFrame(F_SAME1, 0, null, 1, new Object[] { "java/lang/Throwable" });
        mv.visitVarInsn(ASTORE, 8);
        mv.visitVarInsn(ALOAD, 7);
        Label label31 = new Label();
        mv.visitJumpInsn(IFNONNULL, label31);
        mv.visitVarInsn(ALOAD, 8);
        mv.visitVarInsn(ASTORE, 7);
        Label label32 = new Label();
        mv.visitJumpInsn(GOTO, label32);
        mv.visitLabel(label31);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitVarInsn(ALOAD, 8);
        mv.visitJumpInsn(IF_ACMPEQ, label32);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitVarInsn(ALOAD, 8);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "addSuppressed", "(Ljava/lang/Throwable;)V",
                false);
        mv.visitLabel(label32);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitInsn(ATHROW);
        mv.visitLabel(label9);
        mv.visitFrame(F_FULL, 7,
                new Object[] { owner, "java/lang/String", INTEGER, "java/lang/Object",
                        "java/lang/Class", "java/nio/file/Path", "java/lang/String" },
                1, new Object[] { "java/io/IOException" });
        mv.visitVarInsn(ASTORE, 7);
        Label label33 = new Label();
        mv.visitLabel(label33);
        mv.visitLineNumber(36, label33);
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("HotPot error: stubbed loadClas(String, boolean) in ");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        Label label34 = new Label();
        mv.visitLabel(label34);
        mv.visitLineNumber(37, label34);
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("HotPot error: could not define ");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitVarInsn(ALOAD, 6);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        Label label35 = new Label();
        mv.visitLabel(label35);
        mv.visitLineNumber(38, label35);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/IOException", "printStackTrace", "()V", false);
        mv.visitLabel(label18);
        mv.visitLineNumber(13, label18);
        mv.visitFrame(F_CHOP, 3, null, 0, null);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitInsn(MONITOREXIT);
        mv.visitLabel(label13);
        Label label36 = new Label();
        mv.visitJumpInsn(GOTO, label36);
        mv.visitLabel(label12);
        mv.visitFrame(F_SAME1, 0, null, 1, new Object[] { "java/lang/Throwable" });
        mv.visitVarInsn(ALOAD, 3);
        mv.visitInsn(MONITOREXIT);
        mv.visitLabel(label14);
        mv.visitInsn(ATHROW);
        mv.visitLabel(label36);
        mv.visitLineNumber(42, label36);
        mv.visitFrame(F_CHOP, 1, null, 0, null);
    }
}
