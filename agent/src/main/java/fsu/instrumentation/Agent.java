package fsu.instrumentation;

import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;

public class Agent {
    private static Logger LOGGER = LogManager.getLogger();

    public static void premain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[Agent] In agentmain method delegate to agentmain");
        agentmain(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[Agent] In agentmain method");
        try {
            inst.addTransformer(new MyClassTransformer());
        } catch (Exception e) {
            LOGGER.error("Something is wrong!", e);
        }
    }

    public static class MyClassTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader l, String name, Class c, ProtectionDomain d, byte[] b)
                throws IllegalClassFormatException {
            String className = "fsu.jportal.util.MetsUtil".replaceAll("\\.", "/");
            if (name.equals(className)) {
                CustomClassWriter cr = new CustomClassWriter(b);
                return cr.showMethods();
            }
            return b;
        }
    }

    public static class CustomClassWriter {
        ClassReader reader;
        ClassWriter writer;

        public CustomClassWriter(byte[] contents) {
            setReader(new ClassReader(contents));
            setWriter(new ClassWriter(reader, 0));
        }

        public byte[] showMethods() {
            ShowMethodsAdapter showMethodsAdapter = new ShowMethodsAdapter(getWriter());
            getReader().accept(showMethodsAdapter, 0);
            return getWriter().toByteArray();
        }

        public ClassReader getReader() {
            return reader;
        }

        public void setReader(ClassReader reader) {
            this.reader = reader;
        }

        public ClassWriter getWriter() {
            return writer;
        }

        public void setWriter(ClassWriter writer) {
            this.writer = writer;
        }
    }

    public static class ShowMethodsAdapter extends ClassVisitor {
        private static Logger LOGGER = LogManager.getLogger();
        TraceClassVisitor tracer;
        PrintWriter pw = new PrintWriter(System.out);

        public ShowMethodsAdapter(ClassVisitor classVisitor) {
            super(Opcodes.ASM4, classVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
            LOGGER.info("Wisiting method " + name);
            return tracer.visitMethod(access, name, descriptor, signature, exceptions);
        }

        @Override
        public void visitEnd() {
            tracer.visitEnd();
            LOGGER.info(tracer.p.getText());
        }
    }

}
