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
package com.intellij.openapi.ui.popup.util;

import com.intellij.openapi.ui.popup.JBPopup;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class PopupUtil {

  private PopupUtil() {
  }

  @Nullable
  public static Component getOwner(@Nullable Component c) {
    if (c == null) return null;

    final Window wnd = SwingUtilities.getWindowAncestor(c);
    if (wnd instanceof JWindow) {
      final JRootPane root = ((JWindow)wnd).getRootPane();
      final JBPopup popup = (JBPopup)root.getClientProperty(JBPopup.KEY);
      if (popup == null) return c;

      final Component owner = popup.getOwner();
      if (owner == null) return c;

      return getOwner(owner);
    }
    else {
      return c;
    }
  }

}