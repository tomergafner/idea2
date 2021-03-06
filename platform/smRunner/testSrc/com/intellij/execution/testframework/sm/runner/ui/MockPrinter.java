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
package com.intellij.execution.testframework.sm.runner.ui;

import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.testframework.Printable;
import com.intellij.execution.testframework.Printer;
import com.intellij.execution.ui.ConsoleViewContentType;

public class MockPrinter implements Printer {
  private boolean myShouldReset = false;
  private boolean myHasPrinted = false;
  private final StringBuilder myStdOut = new StringBuilder();
  private final StringBuilder myStdErr = new StringBuilder();
  private final StringBuilder myStdSys = new StringBuilder();

  public MockPrinter() {
    this(true);
  }

  public MockPrinter(boolean shouldReset) {
    myShouldReset = shouldReset;
  }

  public void print(String s, ConsoleViewContentType contentType) {
    myHasPrinted = true;
    if (contentType == ConsoleViewContentType.NORMAL_OUTPUT) {
      myStdOut.append(s);
    }
    else if (contentType == ConsoleViewContentType.ERROR_OUTPUT) {
      myStdErr.append(s);
    }
    else if (contentType == ConsoleViewContentType.SYSTEM_OUTPUT) {
      myStdSys.append(s);
    }
  }

  public String getStdOut() {
    return myStdOut.toString();
  }

  public String getStdErr() {
    return myStdErr.toString();
  }

  public String getStdSys() {
    return myStdSys.toString();
  }

  public void setHasPrinted(final boolean hasPrinted) {
    myHasPrinted = hasPrinted;
  }

  public boolean isShouldReset() {
    return myShouldReset;
  }

  public void resetIfNecessary() {
    if (isShouldReset()) {
      myStdErr.setLength(0);
      myStdOut.setLength(0);
      myStdSys.setLength(0);
    }
    setHasPrinted(false);
  }

  public boolean hasPrinted() {
    return myHasPrinted;
  }

  public void onNewAvailable(Printable printable) {
    printable.printOn(this);
  }

  public void printHyperlink(String text, HyperlinkInfo info) {
  }

  public void mark() {}
}
