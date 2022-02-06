package fsu.instrumentation;

import java.io.PrintWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;

public class ShowMethodsAdapter extends ClassVisitor {
    private static Logger LOGGER = LogManager.getLogger();
    TraceClassVisitor tracer;
    PrintWriter pw = new PrintWriter(System.out);

    public ShowMethodsAdapter(ClassVisitor classVisitor) {
        super(Opcodes.ASM4, classVisitor);
        tracer = new TraceClassVisitor(cv, pw);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
            String[] exceptions) {
        LOGGER.info("Visiting method " + name);
        MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
        
        if(name.equals("name")) {
            mv = new NameMethodsAdapter(mv);
        }

        return mv;
    }

    @Override
    public void visitEnd() {
        tracer.visitEnd();
        LOGGER.info(tracer.p.getText());
    }

    public class NameMethodsAdapter extends MethodVisitor {

        public NameMethodsAdapter(MethodVisitor mv) {
            super(Opcodes.ASM4, mv);
        }
    
        
    }
}