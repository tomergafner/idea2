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
package com.intellij.ui;

import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class HighlightableCellRenderer extends HighlightableComponent implements TreeCellRenderer, ListCellRenderer {
  public Component getTreeCellRendererComponent(
    JTree tree,
    Object value,
    boolean selected,
    boolean expanded,
    boolean leaf,
    int row,
    boolean hasFocus
    ) {
    setText(tree.convertValueToText(value, selected, expanded, leaf, row, hasFocus));
    setFont(UIUtil.getTreeFont());
    setIcon(null);

    myIsSelected = selected;
    myHasFocus = hasFocus;
    return this;
  }

  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    setText((value == null) ? "" : value.toString());
    setFont(UIUtil.getListFont());
    setIcon(null);

    myIsSelected = isSelected;
    myHasFocus = cellHasFocus;
    return this;
  }

}
