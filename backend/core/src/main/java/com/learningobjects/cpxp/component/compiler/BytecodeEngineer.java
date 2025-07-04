/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.learningobjects.cpxp.component.compiler;

import com.google.common.collect.MapMaker;
import com.learningobjects.cpxp.component.annotation.Parameter;
import com.learningobjects.cpxp.util.ClassUtils;
import com.learningobjects.cpxp.util.FileUtils;
import org.objectweb.asm.*;

import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class BytecodeEngineer {
    static interface FileAdapter {
        public InputStream openInputStream() throws Exception;

        public OutputStream openOutputStream() throws Exception;
    }

    public static void engineer(final File in, final File out) throws Exception {
        engineer(new FileAdapter() {
            @Override
            public InputStream openInputStream() throws Exception {
                return FileUtils.openInputStream(in);
            }

            @Override
            public OutputStream openOutputStream() throws Exception {
                return FileUtils.openOutputStream(out);
            }
        }, null, null);
    }

    public static void engineer(final JavaFileObject file, final String name, final String smap) throws Exception {
        engineer(new FileAdapter() {
            @Override
            public InputStream openInputStream() throws Exception {
                return file.openInputStream();
            }

            @Override
            public OutputStream openOutputStream() throws Exception {
                return file.openOutputStream();
            }
        }, name, smap);
    }

    private static void engineer(FileAdapter file, final String name, final String smap) throws Exception {

        // First I do a pass to find the parameter names from the
        // debug information... I have to do this in two passes because we
        // visit parameter annotations before we visit the local variable
        // names to fill in parameter names...
        ParameterVisitor pnv = new ParameterVisitor();
        try (final InputStream in = file.openInputStream()) {
            ClassReader pncr = new ClassReader(in);
            pncr.accept(pnv, 0);
        }
        final Map<String, String> parameterNames = pnv.getParameterNames();

        final String PARAMETER_SIG = ClassUtils.getJvmName(Parameter.class);
        // Rewrite line numbers within the class file.
        // Flags: COMPUTE_MAXS, COMPUTE_FRAMES
        // ... none needs to be enabled.
        ClassWriter cw = new ClassWriter(0);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM7,cw) {
            @Override
            public void visitSource(String a, String b) {
                super.visitSource(a, b);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                final int offset = ((access & Opcodes.ACC_STATIC) != 0) ? 0 : 1;
                final String methodKey = name + "/" + desc/*StringUtils.defaultIfEmpty(signature, desc)*/ + "/";
                return new MethodVisitor(Opcodes.ASM7, super.visitMethod(access, name, desc, signature, exceptions)) {
                    @Override
                    public void visitLineNumber(int line, Label start) {
                        super.visitLineNumber(line, start);
                    }

                    @Override
                    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
                        super.visitLocalVariable(name, desc, signature, start, end,
                                index);
                    }

                    @Override
                    public AnnotationVisitor visitParameterAnnotation(final int parameter, final String desc, boolean visible) {
                        final AnnotationVisitor av = super.visitParameterAnnotation(parameter, desc, visible);
                        // If this is an @Parameter annotation and it has
                        // no name parameter then pull the name from the
                        // local variable attributes..
                        return !PARAMETER_SIG.equals(desc) ? av : new AnnotationVisitor(Opcodes.ASM7) {
                            private boolean _named = false;

                            @Override
                            public void visit(String name, Object value) {
                                _named |= "name".equals(name);
                                av.visit(name, value);
                            }

                            @Override
                            public AnnotationVisitor visitAnnotation(String name, String desc) {
                                return av.visitAnnotation(name, desc);
                            }

                            @Override
                            public AnnotationVisitor visitArray(String name) {
                                return av.visitArray(name);
                            }

                            @Override
                            public void visitEnum(String name, String desc, String value) {
                                av.visitEnum(name, desc, value);
                            }

                            @Override
                            public void visitEnd() {
                                if (!_named) {
                                    final String name = parameterNames.get(methodKey + (offset + parameter));
                                    if (name != null) {
                                        av.visit("name", name);
                                    }
                                }
                                av.visitEnd();
                            }
                        };
                    }
                };
            }
        };
        try(final InputStream in = file.openInputStream()) {
            ClassReader cr = new ClassReader(in);
            // Flags: SKIP_DEBUG, EXPAND_FRAMES, SKIP_FRAMES, SKIP_CODE
            // ... none of the SKIPs can be enabled.
            cr.accept(cv, 0);
        }

        if (name != null) {
            cw.visitSource(name, smap);
        }

        try (final OutputStream out = file.openOutputStream()) {
            out.write(cw.toByteArray());
        }
    }

    private static final Map<Class<?>, ParameterNames> _cache =
            new MapMaker().weakKeys().makeMap();

    public static ParameterNames getParameterNames(Class<?> clas) throws IOException {

        ParameterNames value;
        synchronized (_cache) {

            value = _cache.get(clas);
            if (value == null) {

                final String className = ClassUtils.getUnqualifiedName(clas) + ".class";
                final ParameterVisitor pnv = new ParameterVisitor();
                try (final InputStream in = clas.getResourceAsStream(className)) {
                    final ClassReader pncr = new ClassReader(in);
                    pncr.accept(pnv, ClassReader.SKIP_FRAMES);
                }
                value = pnv.getParameterNames();
                _cache.put(clas, value);
            }
        }
        return value;
    }

    public static class ParameterNames extends HashMap<String, String> {
        public String getParameterName(Method method, int i) {
            StringBuilder signature = new StringBuilder(method.getName());
            signature.append("/(");
            for (Class<?> type : method.getParameterTypes()) {
                signature.append(ClassUtils.getJvmName(type));
            }
            signature.append(')');
            signature.append(ClassUtils.getJvmName(method.getReturnType()));
            signature.append('/').append(i + 1);
            String result = get(signature.toString());
            if (result == null) {
                throw new RuntimeException("Unknown method signature: " + signature + " / " + keySet());
            }
            return result;
        }
    }

    public static class ParameterVisitor extends ClassVisitor {
        private final ParameterNames _parameterNames = new ParameterNames();

        public ParameterNames getParameterNames() {
            return _parameterNames;
        }

        public ParameterVisitor(){
            super(Opcodes.ASM7);
        }

        @Override
        public void visitSource(String a, String b) {
            super.visitSource(a, b);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            // If generic, signature will be the generic method signature;
            // otherwise it'll be null and desc will be the non-generic method signature
            final String methodKey = name + "/" + desc/*StringUtils.defaultIfEmpty(signature, desc)*/ + "/";
            return new MethodVisitor(Opcodes.ASM7) {
                @Override
                public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
                   _parameterNames.put(methodKey + index, name);
                }
            };
        }
    }
}
