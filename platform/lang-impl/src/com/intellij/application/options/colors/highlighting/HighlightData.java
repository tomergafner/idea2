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

/**
 * @author Yura Cangea
 */
package com.intellij.application.options.colors.highlighting;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;

import java.awt.*;
import java.util.Map;

public final class HighlightData {
  private final int myStartOffset;
  private int myEndOffset;
  private final TextAttributesKey myHighlightType;

  public HighlightData(int startOffset, TextAttributesKey highlightType) {
    myStartOffset = startOffset;
    myHighlightType = highlightType;
  }

  public HighlightData(int startOffset, int endOffset, TextAttributesKey highlightType) {
    myStartOffset = startOffset;
    myEndOffset = endOffset;
    myHighlightType = highlightType;
  }

  public void addHighlToView(Editor view, EditorColorsScheme scheme, Map<TextAttributesKey,String> displayText) {

    // XXX: Hack
    if (HighlighterColors.BAD_CHARACTER.equals(myHighlightType)) {
      return;
    }

    TextAttributes attr = scheme.getAttributes(myHighlightType);
    if (attr != null) {
      RangeHighlighter highlighter = view.getMarkupModel()
          .addRangeHighlighter(myStartOffset, myEndOffset, HighlighterLayer.ADDITIONAL_SYNTAX, attr, HighlighterTargetArea.EXACT_RANGE);
      final Color errorStripeColor = attr.getErrorStripeColor();
      highlighter.setErrorStripeMarkColor(errorStripeColor);
      final String tooltip = displayText.get(myHighlightType);
      highlighter.setErrorStripeTooltip(tooltip);
    }
  }

  public int getStartOffset() {
    return myStartOffset;
  }

  public int getEndOffset() {
    return myEndOffset;
  }

  public void setEndOffset(int endOffset) {
    myEndOffset = endOffset;
  }

  public String getHighlightType() {
    return myHighlightType.getExternalName();
  }
}
