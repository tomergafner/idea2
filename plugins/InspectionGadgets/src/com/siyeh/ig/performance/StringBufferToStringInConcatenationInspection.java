/*
 * Copyright 2003-2008 Dave Griffith, Bas Leijdekkers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.siyeh.ig.performance;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import com.siyeh.HardcodedMethodConstants;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.psiutils.TypeUtils;
import org.jetbrains.annotations.NotNull;

public class StringBufferToStringInConcatenationInspection
        extends BaseInspection {

    @Override
    @NotNull
    public String getDisplayName() {
        return InspectionGadgetsBundle.message(
                "string.buffer.to.string.in.concatenation.display.name");
    }

    @Override
    @NotNull
    protected String buildErrorString(Object... infos) {
        return InspectionGadgetsBundle.message(
                "string.buffer.to.string.in.concatenation.problem.descriptor");
    }

    @Override
    public BaseInspectionVisitor buildVisitor() {
        return new StringBufferToStringVisitor();
    }

    @Override
    public InspectionGadgetsFix buildFix(Object... infos) {
        return new StringBufferToStringFix();
    }

    private static class StringBufferToStringFix extends InspectionGadgetsFix {

        @NotNull
        public String getName() {
            return InspectionGadgetsBundle.message(
                    "string.buffer.to.string.in.concatenation.remove.quickfix");
        }

        @Override
        public void doFix(Project project, ProblemDescriptor descriptor)
                throws IncorrectOperationException {
            final PsiElement methodNameToken = descriptor.getPsiElement();
            final PsiElement methodCallExpression = methodNameToken.getParent();
            assert methodCallExpression != null;
            final PsiMethodCallExpression methodCall =
                    (PsiMethodCallExpression)methodCallExpression.getParent();
            assert methodCall != null;
            final PsiReferenceExpression expression =
                    methodCall.getMethodExpression();
            final PsiExpression qualifier = expression.getQualifierExpression();
            assert qualifier != null;
            final String newExpression = qualifier.getText();
            replaceExpression(methodCall, newExpression);
        }
    }

    private static class StringBufferToStringVisitor
            extends BaseInspectionVisitor {

        @Override public void visitMethodCallExpression(
                @NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);
            final PsiElement parent = expression.getParent();
            if (!(parent instanceof PsiBinaryExpression)) {
                return;
            }
            final PsiBinaryExpression parentBinary =
                    (PsiBinaryExpression)parent;
            final PsiJavaToken sign = parentBinary.getOperationSign();
            final IElementType tokenType = sign.getTokenType();
            if (!tokenType.equals(JavaTokenType.PLUS)) {
                return;
            }
            final PsiExpression rhs = parentBinary.getROperand();
            if (rhs == null) {
                return;
            }
            if (rhs.equals(expression)) {
                final PsiExpression lhs = parentBinary.getLOperand();
                if (!TypeUtils.expressionHasType("java.lang.String", lhs)) {
                    return;
                }
            } else if (!TypeUtils.expressionHasType("java.lang.String", rhs)) {
                return;
            }
            if (!isStringBufferToString(expression)) {
                return;
            }
            registerMethodCallError(expression);
        }

        private static boolean isStringBufferToString(
                PsiMethodCallExpression expression) {
            final PsiReferenceExpression methodExpression =
                    expression.getMethodExpression();
            final String referenceName = methodExpression.getReferenceName();
            if (!HardcodedMethodConstants.TO_STRING.equals(referenceName)) {
                return false;
            }
            final PsiMethod method = expression.resolveMethod();
            if (method == null) {
                return false;
            }
            final PsiParameterList parameterList = method.getParameterList();
            if (parameterList.getParametersCount() != 0) {
                return false;
            }
            final PsiClass aClass = method.getContainingClass();
            if (aClass == null) {
                return false;
            }
            final String className = aClass.getQualifiedName();
            return "java.lang.StringBuffer".equals(className) ||
                    "java.lang.StringBuilder".equals(className);
        }
    }
}