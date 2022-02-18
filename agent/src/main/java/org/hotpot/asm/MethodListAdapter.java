package org.hotpot.asm;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MethodListAdapter extends ClassVisitor {
    private List<String> methodList;

    public MethodListAdapter(){
        super(Opcodes.ASM8);
        this.methodList = new ArrayList<>();
    }

    public MethodListAdapter(int api, ClassVisitor cv) {
        super(Opcodes.ASM8, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
            String[] exceptions) {
        String method = access+name+descriptor+signature;
        methodList.add(method);
        return null;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        // TODO Auto-generated method stub
        return super.visitField(access, name, descriptor, signature, value);
    }
    
    public List<String> getMethodList() {
        return methodList;
    }
}
