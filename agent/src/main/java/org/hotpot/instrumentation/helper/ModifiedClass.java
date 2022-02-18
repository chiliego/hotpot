package org.hotpot.instrumentation.helper;

import java.nio.file.Path;
import java.util.ArrayList;

import org.hotpot.asm.ASMUtils;

public class ModifiedClass {
    private String nameInternal;
    private String name;
    private byte[] byteCode;
    private Path classFilePath;
    private boolean isModifiable;
    private Class<?> loadedClass;
    private ArrayList<Class<?>> classesWithReference;
    private boolean classExists;

    public ModifiedClass(String name, byte[] byteCode, Path classFilePath) {
        this.nameInternal = name;
        this.name = nameInternal.replace("/", ".");
        this.byteCode = byteCode;
        this.classFilePath = classFilePath;
        this.isModifiable = true;
        this.classesWithReference = new ArrayList<>();
    }

    public ModifiedClass(byte[] byteCode, Path classFilePath) {
        this(ASMUtils.getClassNameInternal(byteCode), byteCode, classFilePath);
    }

    public String getNameInternal() {
        return nameInternal;
    }

    public String getName() {
        return name;
    }

    public byte[] getByteCode() {
        return byteCode;
    }

    public Path getClassFilePath() {
        return classFilePath;
    }

    public Class<?> getLoadedClass() {
        return loadedClass;
    }

    public void setLoadedClass(Class<?> clazz) {
        this.loadedClass = clazz;
    }

    public Class<?>[] getClassesWithRef() {
        return classesWithReference.toArray(new Class<?>[classesWithReference.size()]);
    }

    public void addClassWithRef(Class<?> clazz) {
        classesWithReference.add(clazz);
    }

    public boolean isModifiable() {
        return isModifiable;
    }
    public void setModifiable(boolean isModifiableClass) {
        this.isModifiable = isModifiableClass;
    }

    public boolean classExists() {
        return classExists;
    }

    public void setClassExist(boolean classExists) {
        this.classExists = classExists;
    }
}
