/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.data.generator;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.RETURN;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.spongepowered.api.data.manipulator.DataManipulator;

import java.lang.reflect.Method;

abstract class MethodEntry {

    final Method method;
    final KeyEntry keyEntry;

    MethodEntry(Method method, KeyEntry keyEntry) {
        this.keyEntry = keyEntry;
        this.method = method;
    }

    public void visit(ClassVisitor classVisitor, String targetInternalName, String mutableInternalName) {
        final MethodVisitor mv = classVisitor.visitMethod(ACC_PUBLIC, this.method.getName(),
                Type.getMethodDescriptor(this.method), null, null);
        preVisit(mv, targetInternalName, mutableInternalName);
        mv.visitCode();
        visit(mv, targetInternalName, mutableInternalName);
        mv.visitMaxs(0, 0); // Will be calculated
        mv.visitEnd();
    }

    void preVisit(MethodVisitor mv, String targetInternalName, String mutableInternalName) {
    }

    abstract void visit(MethodVisitor methodVisitor, String targetInternalName, String mutableInternalName);

    void visitSetterEnd(MethodVisitor mv) {
        if (DataManipulator.class.isAssignableFrom(this.method.getReturnType())) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ARETURN);
        } else {
            mv.visitInsn(RETURN);
        }
    }
}
