/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.patterns;

import com.intellij.psi.xml.*;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NonNls;

/**
 * @author peter
 */
public class XmlAttributeValuePattern extends XmlElementPattern<XmlAttributeValue,XmlAttributeValuePattern>{
  private static final InitialPatternCondition<XmlAttributeValue> CONDITION = new InitialPatternCondition<XmlAttributeValue>(XmlAttributeValue.class) {
    public boolean accepts(@Nullable final Object o, final ProcessingContext context) {
      return o instanceof XmlAttributeValue;
    }
  };

  protected XmlAttributeValuePattern() {
    super(CONDITION);
  }

  public XmlAttributeValuePattern withLocalName(@NonNls String... names) {
    return withLocalName(StandardPatterns.string().oneOf(names));
  }

  public XmlAttributeValuePattern withLocalNameIgnoreCase(@NonNls String... names) {
    return withLocalName(StandardPatterns.string().oneOfIgnoreCase(names));
  }

  public XmlAttributeValuePattern withLocalName(ElementPattern<String> namePattern) {
    return with(new PsiNamePatternCondition<XmlAttributeValue>("withLocalName", namePattern) {
      public String getPropertyValue(@NotNull final Object o) {
        if (o instanceof XmlAttributeValue) {
          final XmlAttributeValue value = (XmlAttributeValue)o;
          final PsiElement parent = value.getParent();
          if (parent instanceof XmlAttribute) {
            return ((XmlAttribute)parent).getLocalName();
          }
          if (parent instanceof XmlProcessingInstruction) {
            PsiElement prev = value.getPrevSibling();
            if (!(prev instanceof XmlToken) || ((XmlToken)prev).getTokenType() != XmlTokenType.XML_EQ) return null;
            prev = prev.getPrevSibling();
            if (!(prev instanceof XmlToken) || ((XmlToken)prev).getTokenType() != XmlTokenType.XML_NAME) return null;
            return prev.getText();
          }
        }
        return null;
      }
    });
  }

  public XmlAttributeValuePattern withValue(final StringPattern valuePattern) {
    return with(new PatternCondition<XmlAttributeValue>("withValue") {
      @Override
      public boolean accepts(@NotNull XmlAttributeValue xmlAttributeValue, ProcessingContext context) {
        return valuePattern.accepts(xmlAttributeValue.getValue(), context);
      }
    });
  }

}
