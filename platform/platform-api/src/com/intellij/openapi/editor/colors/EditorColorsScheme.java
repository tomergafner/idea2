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
package com.intellij.openapi.editor.colors;

import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.options.Scheme;
import com.intellij.openapi.util.JDOMExternalizable;

import java.awt.*;

public interface EditorColorsScheme extends Cloneable, JDOMExternalizable, Scheme {
  void setName(String name);

  TextAttributes getAttributes(TextAttributesKey key);
  void setAttributes(TextAttributesKey key, TextAttributes attributes);

  Color getDefaultBackground();
  Color getDefaultForeground();

  Color getColor(ColorKey key);
  void setColor(ColorKey key, Color color);

  int getEditorFontSize();
  void setEditorFontSize(int fontSize);

  String getEditorFontName();
  void setEditorFontName(String fontName);

  Font getFont(EditorFontType key);
  void setFont(EditorFontType key, Font font);

  float getLineSpacing();
  void setLineSpacing(float lineSpacing);

  Object clone();
}
