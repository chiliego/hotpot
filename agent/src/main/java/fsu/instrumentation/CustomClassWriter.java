package fsu.instrumentation;

import java.util.function.Function;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public  class CustomClassWriter {
    ClassReader reader;
    ClassWriter writer;

    public CustomClassWriter(byte[] contents) {
        setReader(new ClassReader(contents));
        setWriter(new ClassWriter(reader, 0));
    }

    public byte[] showMethods() {
        return write(writer -> new ShowMethodsAdapter(writer));
    }

    public byte[] write(Function<ClassWriter, ClassVisitor> withAdapter) {
        ClassVisitor adapter = withAdapter.apply(getWriter());
        getReader().accept(adapter, 0);
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
