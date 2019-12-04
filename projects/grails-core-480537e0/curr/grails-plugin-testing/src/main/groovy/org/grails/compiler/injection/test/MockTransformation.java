/*
 * Copyright 2011 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.compiler.injection.test;

import grails.test.mixin.Mock;

import grails.test.mixin.domain.DomainClassUnitTestMixin;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import java.util.ArrayList;
import java.util.List;

/**
 * Used by the {@link grails.test.mixin.Mock} local transformation to add
 * mocking capabilities for the given classes.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class MockTransformation extends TestForTransformation {

    private static final ClassNode MY_TYPE = new ClassNode(Mock.class);
    private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();

    @Override
    public void visit(ASTNode[] astNodes, SourceUnit source) {
        if (!(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: wrong types: " + astNodes[0].getClass() +
                  " / " + astNodes[1].getClass());
        }

        AnnotatedNode parent = (AnnotatedNode) astNodes[1];
        AnnotationNode node = (AnnotationNode) astNodes[0];
        if (!MY_TYPE.equals(node.getClassNode()) || !(parent instanceof ClassNode)) {
            return;
        }

        ClassNode classNode = (ClassNode) parent;
        String cName = classNode.getName();
        if (classNode.isInterface()) {
            error(source, "Error processing interface '" + cName + "'. " + MY_TYPE_NAME +
                    " not allowed for interfaces.");
        }

        ListExpression values = getListOfClasses(node);
        if (values == null) {
            error(source, "Error processing class '" + cName + "'. " + MY_TYPE_NAME +
                    " annotation expects a class or a list of classes to mock");
            return;
        }

        List<ClassExpression> domainClassNodes = new ArrayList<ClassExpression>();
        for (Expression expression : values.getExpressions()) {
            if (expression instanceof ClassExpression) {
                ClassExpression classEx = (ClassExpression) expression;
                ClassNode cn = classEx.getType();
                Class<?> mixinClassForArtefactType = getMixinClassForArtefactType(cn);
                if (mixinClassForArtefactType != null) {
                    weaveMock(classNode, classEx, false);
                }
                else {
                    domainClassNodes.add(classEx);
                }
            }
        }
        if (!domainClassNodes.isEmpty()) {
            weaveMixinClass(classNode, DomainClassUnitTestMixin.class);
            addMockCollaborators(classNode, "Domain", domainClassNodes);
        }
    }
}
