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

package com.intellij.openapi.roots;

import com.intellij.openapi.module.Module;
import com.intellij.util.containers.HashSet;

import java.util.Set;

/**
 * @author yole
 */
public class RecursiveRootPolicy<R> extends RootPolicy<R> {
  private final Set<Module> myProcessedModules = new HashSet<Module>();

  public R visitModuleOrderEntry(final ModuleOrderEntry moduleOrderEntry, final R value) {
    final Module module = moduleOrderEntry.getModule();
    if (!myProcessedModules.contains(module)) {
      myProcessedModules.add(module);
      return ModuleRootManager.getInstance(module).processOrder(this, value);
    }
    return value;
  }
}
