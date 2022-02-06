package fsu.instrumentation;

import java.io.PrintWriter;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;

public class SwapMethodAdapter extends ClassVisitor {
    private static Logger LOGGER = LogManager.getLogger();
    private String methodName;
    private TraceClassVisitor tracer;
    private boolean traceClass = false;

    private SwapMethodAdapter(ClassVisitor cv) {
        super(Opcodes.ASM4, cv);
    }

    public static Function<ClassWriter, ClassVisitor> forMethod(String methodName) {
        return writer -> SwapMethodAdapter.forMethod(writer, methodName);
    }

    public static Function<ClassWriter, ClassVisitor> forMethod(String methodName, boolean tracing) {
        return writer -> {
            SwapMethodAdapter adapter = SwapMethodAdapter.forMethod(writer, methodName);
            adapter.setTraceClass(tracing);
            return adapter;
        };

    }

    public static SwapMethodAdapter forMethod(ClassVisitor cv, String methodName) {
        SwapMethodAdapter swapMethodAdapter = new SwapMethodAdapter(cv);
        swapMethodAdapter.setMethodName(methodName);
        return swapMethodAdapter;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
            String[] exceptions) {

        MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
        
        if (name.equals(methodName)) {
            //traceMethod(access, name, descriptor, signature, exceptions);
            LOGGER.info("Visit {}", name);
        }

        return mv;
    }

    /*
    @Override
    public void visitEnd() {
        endTraceMethod();
        cv.visitEnd();
    }
    */

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    private void traceMethod(int access, String name, String descriptor, String signature,
            String[] exceptions) {
        if (traceClass) {
            getTracer().visitMethod(access, name, descriptor, signature, exceptions);
        }
    }

    private void endTraceMethod() {
        if (traceClass) {
            getTracer().visitEnd();
            LOGGER.info(getTracer().p.getText());
        }
    }

    private TraceClassVisitor getTracer() {
        if (tracer == null) {
            PrintWriter pw = new PrintWriter(System.out);
            tracer = new TraceClassVisitor(cv, pw);
        }

        return tracer;
    }

    public boolean isTraceClass() {
        return traceClass;
    }

    public void setTraceClass(boolean traceClass) {
        this.traceClass = traceClass;
    }
}
