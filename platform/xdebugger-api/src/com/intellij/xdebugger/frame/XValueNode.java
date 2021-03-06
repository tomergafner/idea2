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
package com.intellij.xdebugger.frame;

import com.intellij.xdebugger.Obsolescent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author nik
 */
public interface XValueNode extends Obsolescent {

  void setPresentation(@NonNls @NotNull String name, @Nullable Icon icon, @NonNls @Nullable String type, @NonNls @NotNull String value, boolean hasChildren);

  void setPresentation(@NonNls @NotNull String name, @Nullable Icon icon, @NonNls @Nullable String type, @NonNls @NotNull String separator, @NonNls @NotNull String value, boolean hasChildren);

}
