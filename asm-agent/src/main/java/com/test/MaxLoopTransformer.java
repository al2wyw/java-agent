package com.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.sf.cglib.core.CodeEmitter;
import net.sf.cglib.core.Local;
import net.sf.cglib.core.Signature;
import net.sf.cglib.transform.ClassEmitterTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Created with IntelliJ IDEA.
 * User: liyang
 * Date: 2023-07-21
 * Time: 16:52
 * Description:
 */
public class MaxLoopTransformer implements ClassFileTransformer {

    private static String outputDir;
    private static String classNameFilter;

    static {
        String dir = System.getProperty("transform.output.dir");
        if (dir != null && !dir.equals("")) {
            outputDir = dir;
        }
        classNameFilter = "pandora";
        String filter = System.getProperty("transform.class.filter");
        if (filter != null && !filter.equals("")) {
            classNameFilter = filter;
        }
        System.out.printf("MaxLoopTransformer outputDir %s, classNameFilter %s\n", outputDir, classNameFilter);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if(!className.contains(classNameFilter)) {
            return classfileBuffer;
        }
        System.out.println("MaxLoopTransformer start to transform " + className);
        ClassReader classReader = new ClassReader(classfileBuffer);

        ClassNode cn = new ClassNode();
        classReader.accept(cn, ClassReader.EXPAND_FRAMES);

        //分析树形结构, 找到对应的label节点
        Set<String> visited = new HashSet<>(); //visited labels
        Set<String> forwardRef = new HashSet<>();//jump forward label reference
        Set<String> backwardRef = new HashSet<>();//jump backward label reference, continue and loop-end-blacket will all backward
        for (MethodNode method : cn.methods) {
            InsnList list = method.instructions;
            AbstractInsnNode node = list.getFirst();
            while (node != null) {
                if (node instanceof LabelNode) {
                    LabelNode label = (LabelNode) node;
                    visited.add(label.getLabel().toString());
                }
                if (node instanceof JumpInsnNode) {
                    JumpInsnNode jump =  (JumpInsnNode) node;
                    LabelNode tar = jump.label;
                    if (visited.contains(tar.getLabel().toString()) && !backwardRef.contains(tar.getLabel().toString())) {
                        backwardRef.add(tar.getLabel().toString());
                        System.out.printf("find the backwardRef %s, %s \n", jump.toString(), tar.getLabel().toString());
                    }
                    if (!visited.contains(tar.getLabel().toString()) && !forwardRef.contains(tar.getLabel().toString())) {
                        forwardRef.add(tar.getLabel().toString());
                        System.out.printf("find the forwardRef %s, %s \n", jump.toString(), tar.getLabel().toString());
                    }
                }
                node = node.getNext();
            }
        }

        if (backwardRef.size() == 0) {
            return classfileBuffer;
        }

        //修改树形结构, 增加nop指令
        for (MethodNode method : cn.methods) {
            //多个跳转指令具有相同的目标label(既有往前跳，也有往回跳)，需要新增一个label进行区分
            Map<String, LabelNode> label2Rep = new HashMap<>();
            InsnList list = method.instructions;
            AbstractInsnNode node = list.getFirst();
            while (node != null) {
                if (node instanceof LabelNode) {
                    LabelNode label = (LabelNode) node;
                    if (backwardRef.contains(label.getLabel().toString())) {
                        System.out.printf("find the loop label %s \n", label.getLabel().toString());
                        //NOP指令用作标识符
                        if (forwardRef.contains(label.getLabel().toString())) {//同时具有前跳和后跳
                            LabelNode rep = new LabelNode();
                            method.instructions.insert(node, new InsnNode(Opcodes.NOP));
                            method.instructions.insert(node, rep);
                            method.instructions.insert(node, new InsnNode(Opcodes.NOP));

                            label2Rep.put(label.getLabel().toString(), rep);
                        } else { //只有后跳
                            method.instructions.insertBefore(node, new InsnNode(Opcodes.NOP));
                            method.instructions.insert(node, new InsnNode(Opcodes.NOP));
                        }
                    }
                }
                if (node instanceof JumpInsnNode) {
                    JumpInsnNode jump =  (JumpInsnNode) node;
                    LabelNode tar = jump.label;
                    if (label2Rep.containsKey(tar.getLabel().toString())) {
                        jump.label = label2Rep.get(tar.getLabel().toString());
                    }
                }
                node = node.getNext();
            }
        }

        //transform
        ClassEmitterTransformer cv = new ClassEmitterTransformer() {

            @Override
            public CodeEmitter begin_method(int access, Signature sig, Type[] exceptions) {
                CodeEmitter codeEmitter = super.begin_method(access, sig, exceptions);

                return new ByteCodeMaxLoopEmitter(codeEmitter);
            }
        };
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        cv.setTarget(classWriter);

        cn.accept(cv);
        byte[] content = classWriter.toByteArray();
        if (outputDir != null) {
            try {
                String file = outputDir + File.separator + className + ".class";
                FileOutputStream fout = new FileOutputStream(file);
                fout.write(content);
                fout.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return content;
    }

    public static class ByteCodeMaxLoopEmitter extends CodeEmitter {

        private boolean closed = false;
        private Local local = null;

        public ByteCodeMaxLoopEmitter(CodeEmitter wrap) {
            super(wrap);
        }

        @Override public void visitInsn(int opcode) {
            if (Opcodes.NOP == opcode) {
                if (closed) {
                    load_local(local);
                    invoke_static(Type.getType(MaxLoopChecker.class),
                            new Signature("checkLoop", Type.VOID_TYPE, new Type[]{Type.INT_TYPE}));
                    iinc(local, 1);
                    closed = false;
                } else {
                    local = make_local(Type.INT_TYPE);
                    push(0);
                    store_local(local);
                    closed = true;
                }
                return;
            }
            super.visitInsn(opcode);
        }
    }
}
