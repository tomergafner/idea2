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
package com.siyeh.ig.bugs;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.siyeh.HardcodedMethodConstants;
import org.jetbrains.annotations.NotNull;

class ParameterClassCheckVisitor extends JavaRecursiveElementVisitor{

    private final PsiParameter parameter;

    private boolean checked = false;

    ParameterClassCheckVisitor(PsiParameter parameter){
        super();
        this.parameter = parameter;
    }

    @Override public void visitElement(@NotNull PsiElement element){
        if(!checked){
            super.visitElement(element);
        }
    }

    @Override public void visitMethodCallExpression(
            @NotNull PsiMethodCallExpression expression){
        if(checked){
            return;
        }
        super.visitMethodCallExpression(expression);
        final PsiReferenceExpression methodExpression =
                expression.getMethodExpression();
        final String methodName = methodExpression.getReferenceName();
        if(!HardcodedMethodConstants.GET_CLASS.equals(methodName)){
            return;
        }
        final PsiExpressionList argumentList = expression.getArgumentList();
        final PsiExpression[] arguments = argumentList.getExpressions();
        if(arguments.length != 0){
            return;
        }
        final PsiExpression qualifier =
                methodExpression.getQualifierExpression();
        if(isParameterReference(qualifier)){
            checked = true;
        }
    }

    @Override public void visitInstanceOfExpression(
            @NotNull PsiInstanceOfExpression expression){
        if(checked){
            return;
        }
        super.visitInstanceOfExpression(expression);
        final PsiExpression operand = expression.getOperand();
        if(isParameterReference(operand)){
            checked = true;
        }
    }

    @Override public void visitTypeCastExpression(
            PsiTypeCastExpression expression) {
        if(checked){
            return;
        }
        super.visitTypeCastExpression(expression);
        final PsiExpression operand = expression.getOperand();
        if(!isParameterReference(operand)){
            return;
        }
        final PsiTryStatement statement =
                PsiTreeUtil.getParentOfType(expression, PsiTryStatement.class);
        if(statement == null){
            return;
        }
        final PsiParameter[] parameters = statement.getCatchBlockParameters();
        if(parameters.length < 2){
            return;
        }
        boolean nullPointerExceptionFound = false;
        boolean classCastExceptionFound = false;
        for(PsiParameter parameter : parameters){
            final PsiType type = parameter.getType();
            if(type.equalsToText("java.lang.NullPointerException")){
                nullPointerExceptionFound = true;
                if(classCastExceptionFound){
                    break;
                }
            } else if(type.equalsToText("java.lang.ClassCastException")){
                classCastExceptionFound = true;
                if(nullPointerExceptionFound){
                    break;
                }
            }
        }
        if(classCastExceptionFound && nullPointerExceptionFound){
            checked = true;
        }
    }

    private boolean isParameterReference(PsiExpression operand){
        if(operand == null){
            return false;
        }
        if(!(operand instanceof PsiReferenceExpression)){
            return false;
        }
        final PsiReferenceExpression expression =
                (PsiReferenceExpression)operand;
        final PsiElement referent = expression.resolve();
        return referent != null && referent.equals(parameter);
    }

    public boolean isChecked(){
        return checked;
    }
}