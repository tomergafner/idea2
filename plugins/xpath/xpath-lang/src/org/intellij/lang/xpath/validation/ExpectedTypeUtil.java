/*
 * Copyright 2005 Sascha Weinreuter
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

/*
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 04.05.2006
 * Time: 22:32:31
 */
package org.intellij.lang.xpath.validation;

import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.intellij.lang.xpath.XPathElementType;
import org.intellij.lang.xpath.XPathTokenTypes;
import org.intellij.lang.xpath.context.ContextProvider;
import org.intellij.lang.xpath.context.functions.Function;
import org.intellij.lang.xpath.context.functions.Parameter;
import org.intellij.lang.xpath.psi.*;

public class ExpectedTypeUtil {
    private ExpectedTypeUtil() {
    }

    @NotNull
    public static XPathType getExpectedType(XPathExpression expression) {
        XPathType expectedType = XPathType.UNKNOWN;
        final XPathExpression parentExpr = PsiTreeUtil.getParentOfType(expression, XPathExpression.class);
        if (parentExpr != null) {
            if (parentExpr instanceof XPathBinaryExpression) {
                final XPathElementType op = ((XPathBinaryExpression)parentExpr).getOperator();
                if (op == XPathTokenTypes.AND || op == XPathTokenTypes.OR) {
                    expectedType = XPathType.BOOLEAN;
                } else if (XPathTokenTypes.NUMBER_OPERATIONS.contains(op)) {
                    expectedType = XPathType.NUMBER;
                } else {
                    expectedType = XPathType.UNKNOWN;
                }
            } else if (parentExpr instanceof XPathFunctionCall) {
                final XPathFunctionCall call = (XPathFunctionCall)parentExpr;
                final XPathFunction xpathFunction = call.resolve();
                if (xpathFunction != null) {
                    final Function functionDecl = xpathFunction.getDeclaration();
                    if (functionDecl != null) {
                        final Parameter p = findParameterDecl(call.getArgumentList(), expression, functionDecl.parameters);
                        if (p != null) {
                            expectedType = p.type;
                        }
                    }
                }
            } else if (parentExpr instanceof XPathStep) {
                final XPathStep step = (XPathStep)parentExpr;
                final XPathPredicate[] predicates = step.getPredicates();
                for (XPathPredicate predicate : predicates) {
                    if (predicate.getPredicateExpression() == expression) {
                        return getPredicateType(expression);
                    }
                }
                expectedType = XPathType.NODESET;
            } else if (parentExpr instanceof XPathLocationPath) {
                expectedType = XPathType.NODESET;
            } else if (parentExpr instanceof XPathFilterExpression) {
                final XPathFilterExpression filterExpression = (XPathFilterExpression)parentExpr;

                if (filterExpression.getExpression() == expression) {
                    return XPathType.NODESET;
                }

                assert ((XPathFilterExpression)parentExpr).getPredicate().getPredicateExpression() == expression;

                return getPredicateType(expression);
            }
        } else {
            expectedType = ContextProvider.getContextProvider(expression).getExpectedType(expression);
        }
        return expectedType;
    }

    public static XPathType getPredicateType(XPathExpression expression) {
        // special: If the result is a number, the result will be converted to true if the number is equal to
        // the context position and will be converted to false otherwise;
        // (http://www.w3.org/TR/xpath#predicates)
        return expression.getType() == XPathType.NUMBER ? XPathType.NUMBER : XPathType.BOOLEAN;
    }

    @Nullable
    private static Parameter findParameterDecl(XPathExpression[] argumentList, XPathExpression expr, Parameter[] parameters) {
        for (int i = 0; i < argumentList.length; i++) {
            XPathExpression arg = argumentList[i];
            if (arg == expr) {
                if (i < parameters.length) {
                    return parameters[i];
                } else if (parameters.length > 0) {
                    final Parameter last = parameters[parameters.length - 1];
                    if (last.kind == Parameter.Kind.VARARG) {
                        return last;
                    }
                }
            }
        }
        return null;
    }

    public static boolean isExplicitConversion(XPathExpression expression) {
        expression = unparenthesize(expression);

        if (!(expression instanceof XPathFunctionCall)) {
            return false;
        }
        final XPathFunctionCall call = ((XPathFunctionCall)expression);
        if (call.getQName().getPrefix() != null || call.getArgumentList().length != 1) {
            return false;
        }
        return XPathType.fromString(call.getFunctionName()) != XPathType.UNKNOWN;
    }

    // TODO: put this somewhere else
    @Nullable
    public static XPathExpression unparenthesize(XPathExpression expression) {
        while (expression instanceof XPathParenthesizedExpression) {
            expression = ((XPathParenthesizedExpression)expression).getExpression();
        }
        return expression;
    }
}
