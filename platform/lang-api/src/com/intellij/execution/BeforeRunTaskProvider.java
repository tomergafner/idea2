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

/*
 * Created by IntelliJ IDEA.
 * User: Vladislav.Kaznacheev
 * Date: Jul 4, 2007
 * Time: 12:33:18 AM
 */
package com.intellij.execution;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

public interface BeforeRunTaskProvider<T extends BeforeRunTask> {
  @NonNls ExtensionPointName<BeforeRunTaskProvider<BeforeRunTask>> EXTENSION_POINT_NAME = new ExtensionPointName<BeforeRunTaskProvider<BeforeRunTask>>("com.intellij.stepsBeforeRunProvider");

  Key<T> getId();

  String getDescription(final RunConfiguration runConfiguration, T task);

  boolean hasConfigurationButton();

  // lifecycle methods:
  /**
   * @return 'before run' task for the configuration or null, if the task from this provider is not applicable to the specified configuration 
   */
  @Nullable
  T createTask(final RunConfiguration runConfiguration);
  
  void configureTask(final RunConfiguration runConfiguration, T task);

  boolean executeTask(DataContext context, RunConfiguration configuration, T task);
}