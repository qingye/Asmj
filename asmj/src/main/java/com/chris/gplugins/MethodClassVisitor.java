package com.chris.gplugins;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Created by chris on 2017/12/20.
 */

public class MethodClassVisitor extends ClassVisitor {

    private String className;

    public MethodClassVisitor(String className, ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new MethodAdviceAdapter(cv.visitMethod(access, name, desc, signature, exceptions), className, access, name, desc);
    }
}
