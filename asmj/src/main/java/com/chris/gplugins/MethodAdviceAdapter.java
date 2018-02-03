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

    private boolean isInnerClass = false;
    private String className;
    private String normalName;
    private String methodName;

    protected MethodAdviceAdapter(MethodVisitor mv, String className, int access, String name, String desc) {
        super(Opcodes.ASM5, mv, access, name, desc);
        this.className = className;
        this.normalName = className.replaceAll("/", ".");
        this.methodName = name;
        this.isInnerClass = className.contains("$");
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
        if (isInnerClass && methodName.equals("<init>")) {
            return;
        }

        String str = normalName + "_" + methodName + "_" + methodDesc;
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
        if (isInnerClass && methodName.equals("<init>")) {
            return;
        }
        System.out.println("[aspectEnd]");

        String str = normalName + "_" + methodName + "_" + methodDesc;
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
//            gatherVariants(true, list);
            gatherVariants(false, list);
        }
    }

    /***********************************************************************************************
     * Call like this:
     * 1. stack-memory （isLocal: true）
     *     com.chris.sdklib.ClassTracker.onEvents(new Object[]{instance, var1, var2, ..., varN});
     *
     * 2. heap-memory  （isLocal: false）
     *     Object[] var = new Object[desc.size()]
     *     var[0] = instance;
     *     var[1] = var1;
     *     ...
     *     var[M] = varN;
     *     com.chris.sdklib.ClassTracker.onEvents(var);
     ***********************************************************************************************/
    private void gatherVariants(boolean isLocal, List<String> desc) {
        if (desc != null && desc.size() > 0) {

            /***************************************************************************************
             * 申请数组内存，大小 = desc.size() + 2 [+1 optional]
             ***************************************************************************************/
            malloc(desc.size() + 3);
            if (isLocal) {
                mv.visitVarInsn(ASTORE, 1 + desc.size()); // 临时数组变量存到栈中
            }

            /***************************************************************************************
             * 将对象实例放入数组[0]
             ***************************************************************************************/
            if (isLocal) {
                mv.visitVarInsn(ALOAD, 1 + desc.size());
            } else {
                mv.visitInsn(DUP);
            }
            storeClassInstance();

            /***************************************************************************************
             * 将类名、方法名、本地变量（形参）存入数组中[1]...[N]
             ***************************************************************************************/
            storeString(desc, isLocal, 1, normalName);
            storeString(desc, isLocal, 2, methodName);
            storeMethodVariant(desc, isLocal);

            /***************************************************************************************
             * 注入埋点
             ***************************************************************************************/
            if (isLocal) {
                mv.visitVarInsn(ALOAD, 1 + desc.size());
            }
            callThirdMethod("onEvents");
        }
    }

    /***********************************************************************************************
     * 获取外部类对象实例
     ***********************************************************************************************/
    private void storeClassInstance() {
        if (isInnerClass) {
            String outerClass = className.split("\\$")[0];
            mv.visitIntInsn(BIPUSH, 0);
            mv.visitVarInsn(ALOAD, 0); // 当前内部类对象实例
            mv.visitFieldInsn(GETFIELD, className, "this$0", "L" + outerClass + ";");
            mv.visitInsn(AASTORE);
        } else {
            mv.visitIntInsn(BIPUSH, 0);
            mv.visitVarInsn(ALOAD, 0); // 当前类对象实例
            mv.visitInsn(AASTORE);
        }
    }

    /***********************************************************************************************
     * 获取当前方法的参数
     ***********************************************************************************************/
    private void storeMethodVariant(List<String> desc, boolean isLocal) {
        for (int i = 0; i < desc.size(); i ++) {
            if (isLocal) {
                mv.visitVarInsn(ALOAD, 1 + desc.size());
            } else {
                mv.visitInsn(DUP);
            }
            mv.visitIntInsn(BIPUSH, i + 3);
            mv.visitVarInsn(ALOAD, i + 1);
            mv.visitInsn(AASTORE);
        }
    }

    /***********************************************************************************************
     * 堆上分配内存
     ***********************************************************************************************/
    private void malloc(int len) {
        mv.visitIntInsn(BIPUSH, len);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    }

    /***********************************************************************************************
     * 存储字符串
     ***********************************************************************************************/
    private void storeString(List<String> desc, boolean isLocal, int offset, String name) {
        if (isLocal) {
            mv.visitVarInsn(ALOAD, 1 + desc.size());
        } else {
            mv.visitInsn(DUP);
        }
        mv.visitIntInsn(BIPUSH, offset);
        mv.visitLdcInsn(name);
        mv.visitInsn(AASTORE);
    }

    /***********************************************************************************************
     * 解析参数类型
     ***********************************************************************************************/
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

    /***********************************************************************************************
     * 堆上分配内存
     ***********************************************************************************************/
    private void callThirdMethod(String md) {
        if (md.equals("onEvents")) {
            mv.visitMethodInsn(INVOKESTATIC, "com/chris/sdklib/ClassTracker", "onEvents", "([Ljava/lang/Object;)V", false);
        }
    }
}
