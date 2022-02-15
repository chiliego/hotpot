package org.hotpot.instrumentation.transformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hotpot.asm.ASMUtils;
import org.hotpot.asm.AddHelloLoggerMV;
import org.hotpot.asm.MethodReplacer;
import org.objectweb.asm.ClassVisitor;

public class ClassLoaderTransformer implements ClassFileTransformer {
    private static Logger LOGGER = LogManager.getLogger(ClassLoaderTransformer.class);
    private List<Class<?>> classLoaders;
    private Class<?> targetCls;

    public ClassLoaderTransformer(Class<?> targetclassLoaderClass) {
        this.targetCls = targetclassLoaderClass;
    }

    @Override
    public byte[] transform(ClassLoader l, String name, Class<?> clazz, ProtectionDomain pd, byte[] b)
            throws IllegalClassFormatException {
                
            if(clazz.equals(targetCls)) {
                String methodName = "loadClass";
                LOGGER.info("Transform classloader [{}]", clazz.getName());
                UnaryOperator<ClassVisitor> methodReplacer = cv -> new MethodReplacer(cv, n -> n.equals(methodName), AddHelloLoggerMV::new);
                return ASMUtils.applyClassVisitor(b, methodReplacer, false);
            }

        return null;
    }

    private boolean isClassLoader(Class<?> clazz) {
        Class<?> superclass = clazz.getSuperclass();

        if(superclass == null) {
            return false;
        } else if(superclass.equals(ClassLoader.class)) {
            return true;
        } else {
            return isClassLoader(superclass);
        }
    }

    public List<Class<?>> getClassLoaders() {
        if (classLoaders == null) {
            classLoaders = new ArrayList<>();
        }

        return classLoaders;
    }

    public Class<?>[] getClassLoaderArray() {
        List<Class<?>> classLoaderList = getClassLoaders();

        return classLoaderList.toArray(new Class<?>[classLoaderList.size()]);
    }
}
