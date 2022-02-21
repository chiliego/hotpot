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
    private ClassLoader classloader;
    private String superName;
    private boolean isSubClass;

    public ModifiedClass(String name, byte[] byteCode, Path classFilePath) {
        this.nameInternal = name;
        this.name = nameInternal.replace("/", ".");
        this.byteCode = byteCode;
        this.classFilePath = classFilePath;
        this.isModifiable = true;
        this.isSubClass = false;
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
        setClassloader(clazz.getClassLoader());
    }

    public Class<?>[] getClassesWithRef() {
        return classesWithReference.toArray(new Class<?>[classesWithReference.size()]);
    }

    public void addClassWithRef(Class<?> clazz) {
        if(getClassloader() == null) {
            setClassloader(clazz.getClassLoader());
        }

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

    public ClassLoader getClassloader() {
        return classloader;
    }

    public void setClassloader(ClassLoader classloader) {
        this.classloader = classloader;
    }

    public boolean isSubclass() {
        return isSubClass;
    }

    public String getSuperName() {
        return superName;
    }

    public void makeSubclass(String subClassName, byte[] subClassByteCode, Path subClassFilePath) {
        this.isSubClass = true;
        this.superName = this.name;
        this.name = subClassName;
        this.byteCode = subClassByteCode;
        this.classFilePath = subClassFilePath;
    }
}
