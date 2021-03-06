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
package com.intellij.application.options;

import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.psi.codeStyle.CodeStyleSettings;

import javax.swing.*;

/**
 * @author yole
 */
public class JavaIndentOptionsEditor extends SmartIndentOptionsEditor {
  private JTextField myLabelIndent;
  private JLabel myLabelIndentLabel;

  private JCheckBox myLabelIndentAbsolute;
  private JCheckBox myCbDontIndentTopLevelMembers;

  protected void addComponents() {
    super.addComponents();

    myLabelIndent = new JTextField(4);
    add(myLabelIndentLabel = new JLabel(ApplicationBundle.message("editbox.indent.label.indent")), myLabelIndent);

    myLabelIndentAbsolute = new JCheckBox(ApplicationBundle.message("checkbox.indent.absolute.label.indent"));
    add(myLabelIndentAbsolute, true);

    myCbDontIndentTopLevelMembers = new JCheckBox(ApplicationBundle.message("checkbox.do.not.indent.top.level.class.members"));
    add(myCbDontIndentTopLevelMembers);
  }

  public boolean isModified(final CodeStyleSettings settings, final CodeStyleSettings.IndentOptions options) {
    boolean isModified = super.isModified(settings, options);

    isModified |= isFieldModified(myLabelIndent, options.LABEL_INDENT_SIZE);
    isModified |= isFieldModified(myLabelIndentAbsolute, options.LABEL_INDENT_ABSOLUTE);
    isModified |= isFieldModified(myCbDontIndentTopLevelMembers, settings.DO_NOT_INDENT_TOP_LEVEL_CLASS_MEMBERS);

    return isModified;
  }

  public void apply(final CodeStyleSettings settings, final CodeStyleSettings.IndentOptions options) {
    super.apply(settings, options);
    try {
      options.LABEL_INDENT_SIZE = Integer.parseInt(myLabelIndent.getText());
    }
    catch (NumberFormatException e) {
      //stay with default
    }
    options.LABEL_INDENT_ABSOLUTE = myLabelIndentAbsolute.isSelected();
    settings.DO_NOT_INDENT_TOP_LEVEL_CLASS_MEMBERS = myCbDontIndentTopLevelMembers.isSelected();
  }

  public void reset(final CodeStyleSettings settings, final CodeStyleSettings.IndentOptions options) {
    super.reset(settings, options);
    myLabelIndent.setText(Integer.toString(options.LABEL_INDENT_SIZE));
    myLabelIndentAbsolute.setSelected(options.LABEL_INDENT_ABSOLUTE);
    myCbDontIndentTopLevelMembers.setSelected(settings.DO_NOT_INDENT_TOP_LEVEL_CLASS_MEMBERS);
  }

  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    myLabelIndent.setEnabled(enabled);
    myLabelIndentLabel.setEnabled(enabled);
    myLabelIndentAbsolute.setEnabled(enabled);
  }
}
