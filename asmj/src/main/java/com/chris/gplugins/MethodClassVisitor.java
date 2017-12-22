package com.chris.gplugins;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

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
        MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
        methodVisitor = new AdviceAdapter(Opcodes.ASM5, methodVisitor, access, name, desc) {
            @Override
            protected void onMethodEnter() {
                aspectBegin();
            }

            @Override
            protected void onMethodExit(int opcode) {
                aspectEnd();
            }

            private void aspectBegin() {
                if (mv == null) {
                    return;
                }

                String str = className + "_" + name + "_" + desc;
                System.out.println(str + " => [aspectBegin]");
                mv.visitLdcInsn(str);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
                mv.visitMethodInsn(INVOKESTATIC, "com/chris/sdklib/MethodCost", "setTimeStart", "(Ljava/lang/String;J)V", false);

                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false);

                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                mv.visitLdcInsn("-------------- [" + str + "] begin --------------");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            }

            private void aspectEnd() {
                if (mv == null) {
                    return;
                }
                System.out.println("[aspectEnd]");

                String str = className + "_" + name + "_" + desc;
                mv.visitLdcInsn(str);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
                mv.visitMethodInsn(INVOKESTATIC, "com/chris/sdklib/MethodCost", "setTimeEnd", "(Ljava/lang/String;J)V", false);

                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                mv.visitLdcInsn("-------------- [" + str + "] end ----------------");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                mv.visitLdcInsn(str);
                mv.visitMethodInsn(INVOKESTATIC, "com/chris/sdklib/MethodCost", "getTimeCost", "(Ljava/lang/String;)Ljava/lang/String;", false);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            }
        };
        return methodVisitor;
    }
}
