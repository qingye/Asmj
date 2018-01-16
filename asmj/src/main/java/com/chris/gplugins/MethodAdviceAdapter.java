package com.chris.gplugins;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by chris on 2017/12/28.
 */

public class MethodAdviceAdapter extends AdviceAdapter {

    private String className;
    private String methodName;

    protected MethodAdviceAdapter(MethodVisitor mv, String className, int access, String name, String desc) {
        super(Opcodes.ASM5, mv, access, name, desc);
        this.className = className;
        this.methodName = name;
    }

    @Override
    protected void onMethodEnter() {
        if (mv == null) {
            return;
        }
        aopBegin();
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (mv == null) {
            return;
        }
        aopEnd();
    }

    private void aopBegin() {
        /* 内部类 & 内部类默认构造函数 */
        if (className.contains("$") && methodName.equals("<init>")) {
            return;
        }

        String str = className + "_" + methodName + "_" + methodDesc;
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

        methedOnEvent();
    }

    private void aopEnd() {
        if (className.contains("$") && methodName.equals("<init>")) {
            return;
        }
        System.out.println("[aspectEnd]");

        String str = className + "_" + methodName + "_" + methodDesc;
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

    private void methedOnEvent() {
        if (methodName.equals("onClick") || methodName.equals("onTouch")) {
            List<String> list = parseDesc();
            createLocalArray(list);
            createTempArray(list);
        }
    }

    private void createLocalArray(List<String> desc) {
        if (desc != null && desc.size() > 0) {

            /****************************************************************
             * 本地创建临时数组
             * 大小 = desc.size()
             ****************************************************************/
            mv.visitIntInsn(BIPUSH, desc.size());
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

            /****************************************************************
             * 入栈 => 本地变量的最后
             * [0] = this;
             * [1] ... [N] = desc.getIndex(0) ... desc.getIndex(size - 1);
             * [N + 1] = 创建的临时变量
             ****************************************************************/
            mv.visitVarInsn(ASTORE, 1 + desc.size());

            /****************************************************************
             * 将本地变量（形参）存入数组中[1]...[N]
             ****************************************************************/
            for (int i = 1; i <= desc.size(); i ++) {
                mv.visitVarInsn(ALOAD, 1 + desc.size());
                mv.visitIntInsn(BIPUSH, i - 1);
                mv.visitVarInsn(ALOAD, i);
                mv.visitInsn(AASTORE);
            }

            mv.visitVarInsn(ALOAD, 1 + desc.size());
            mv.visitMethodInsn(INVOKESTATIC, "com/chris/sdklib/ClassTracker", "onEvents", "([Ljava/lang/Object;)V", false);
        }
    }

    private void createTempArray(List<String> desc) {
        if (desc != null && desc.size() > 0) {
            /****************************************************************
             * 本地创建内存临时数组
             * 大小 = desc.size()
             ****************************************************************/
            mv.visitIntInsn(BIPUSH, desc.size() + 1);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

            /****************************************************************
             * 将对象实例放入数组[0]
             ****************************************************************/
            if (className.contains("$")) {
                String outerClass = className.split("\\$")[0];

                mv.visitInsn(DUP);
                mv.visitIntInsn(BIPUSH, 0);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "this$0", "L" + outerClass + ";");
                mv.visitInsn(AASTORE);
            }

            /****************************************************************
             * 将本地变量（形参）存入数组中[1]...[N]
             ****************************************************************/
            for (int i = 1; i <= desc.size(); i ++) {
                mv.visitInsn(DUP);
                mv.visitIntInsn(BIPUSH, i);
                mv.visitVarInsn(ALOAD, i);
                mv.visitInsn(AASTORE);
            }

            mv.visitVarInsn(ALOAD, 1 + desc.size());
            mv.visitMethodInsn(INVOKESTATIC, "com/chris/sdklib/ClassTracker", "onEvents", "([Ljava/lang/Object;)V", false);
        }
    }

    private List<String> parseDesc() {
        String desc = methodDesc.substring(1, methodDesc.length()).split("\\)")[0];
        List<String> list = new ArrayList<>();
        String param = "";
        while (desc.length() > 0) {
            int indexOf = -1;
            switch (desc.charAt(0)) {
                case 'Z':  // boolean
                case 'C':  // char
                case 'B':  // byte
                case 'S':  // short
                case 'I':  // int
                case 'F':  // float
                case 'J':  // long
                case 'D':  // double
                    indexOf = 0;
                case 'L':  // full path Object
                    indexOf = indexOf == 0 ? indexOf : desc.indexOf(";");
                    list.add(param + desc.substring(0, indexOf + 1));
                    desc = desc.substring(indexOf + 1, desc.length());
                    param = "";
                    break;

                case '[':  // array
                    param += "[";
                    desc = desc.substring(1, desc.length());
                    break;

                default:
                    System.out.println("No match..");
                    break;
            }
        }
        return list;
    }
}
