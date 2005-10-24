/*
 * Copyright 2000-2005 JetBrains s.r.o.
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
package org.jetbrains.idea.devkit.module;

import com.intellij.openapi.module.ModuleComponent;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.roots.ui.configuration.DefaultModuleConfigurationEditorFactory;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationEditorProvider;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import org.jetbrains.idea.devkit.build.PluginModuleBuildConfEditor;

import java.util.ArrayList;
import java.util.List;

public class PluginModuleEditorsProvider implements ModuleComponent, ModuleConfigurationEditorProvider{
  public String getComponentName() {
    return "DevKit.PluginModuleEditorsProvider";
  }


  public ModuleConfigurationEditor[] createEditors(ModuleConfigurationState state) {
    final DefaultModuleConfigurationEditorFactory editorFactory = DefaultModuleConfigurationEditorFactory.getInstance();
    ModulesProvider provider = state.getModulesProvider();
    List<ModuleConfigurationEditor> editors = new ArrayList<ModuleConfigurationEditor>();
    editors.add(editorFactory.createModuleContentRootsEditor(state));
    //editors.add(editorFactory.createLibrariesEditor(state));
    //if (provider.getModules().length > 1) {
    //  editors.add(editorFactory.createDependenciesEditor(state));
    //}
    //editors.add(editorFactory.createOrderEntriesEditor(state));
    editors.add(editorFactory.createClasspathEditor(state));
    editors.add(editorFactory.createJavadocEditor(state));
    editors.add(new PluginModuleBuildConfEditor(state));
    return editors.toArray(new ModuleConfigurationEditor[editors.size()]);
  }

  public void projectOpened() {}
  public void projectClosed() {}
  public void moduleAdded() {}
  public void initComponent() {}
  public void disposeComponent() {}
}