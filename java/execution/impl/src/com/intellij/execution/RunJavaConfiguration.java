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
package com.intellij.execution;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

public interface RunJavaConfiguration {
  int VM_PARAMETERS_PROPERTY = 0;
  int PROGRAM_PARAMETERS_PROPERTY = 1;
  int WORKING_DIRECTORY_PROPERTY = 2;

  void setProperty(int property, String value);
  String getProperty(int property);

  Project getProject();

  boolean isAlternativeJrePathEnabled();

  void setAlternativeJrePathEnabled(boolean enabled);

  String getAlternativeJrePath();

  void setAlternativeJrePath(String ALTERNATIVE_JRE_PATH);

  @Nullable
  String getRunClass();

  @Nullable
  String getPackage();

}
