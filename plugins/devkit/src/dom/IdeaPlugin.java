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

// Generated on Wed Nov 07 17:26:02 MSK 2007
// DTD/Schema  :    plugin.dtd

package org.jetbrains.idea.devkit.dom;

import com.intellij.util.xml.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * plugin.dtd:idea-plugin interface.
 */
@DefinesXml
public interface IdeaPlugin extends DomElement {
  @Nullable
  String getPluginId();

  @NotNull
  @NameValue
  GenericDomValue<String> getId();

  @NotNull
  GenericAttributeValue<String> getVersion();

  @NotNull
  GenericAttributeValue<String> getUrl();

  @NotNull
  GenericAttributeValue<Boolean> getUseIdeaClassloader();

  @NotNull
  GenericDomValue<String> getName();


  @NotNull
  List<GenericDomValue<String>> getDescriptions();
  GenericDomValue<String> addDescription();


  @NotNull
  List<GenericDomValue<String>> getVersions();
  GenericDomValue<String> addVersion();


  @NotNull
  List<Vendor> getVendors();
  Vendor addVendor();


  @NotNull
  List<GenericDomValue<String>> getChangeNotess();
  GenericDomValue<String> addChangeNotes();


  @NotNull
  List<IdeaVersion> getIdeaVersions();
  IdeaVersion addIdeaVersion();


  @NotNull
  List<GenericDomValue<String>> getCategories();
  GenericDomValue<String> addCategory();


  @NotNull
  List<GenericDomValue<String>> getResourceBundles();
  GenericDomValue<String> addResourceBundle();


  @NotNull
  @SubTagList("depends")
  List<Dependency> getDependencies();
  @SubTagList("depends")
  Dependency addDependency();


  @NotNull
  @SubTagList("extensions")
  List<Extensions> getExtensions();
  Extensions addExtensions();

  @NotNull
  @SubTagList("extensionPoints")
  List<ExtensionPoints> getExtensionPoints();


  @NotNull
  ApplicationComponents getApplicationComponents();

  @NotNull
  ProjectComponents getProjectComponents();

  @NotNull
  ModuleComponents getModuleComponents();


  @NotNull
  Actions getActions();


  @NotNull
  List<Helpset> getHelpsets();

  Helpset addHelpset();
}
