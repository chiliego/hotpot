package org.hotpot.asm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class ClassNameAdapter extends ClassVisitor {
    private static Logger LOGGER = LogManager.getLogger(ClassNameAdapter.class);
    private String className;

    public ClassNameAdapter() {
        super(Opcodes.ASM8);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
            String[] interfaces) {
        LOGGER.info("Class name [{}]", name);
        this.className = name;
    }

    public String getClassName() {
        return className;
    }
}
