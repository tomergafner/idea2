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

package com.intellij.lang.documentation;

import com.intellij.openapi.extensions.Extensions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CompositeDocumentationProvider implements DocumentationProvider{

  private final List<DocumentationProvider> myProviders;

  public CompositeDocumentationProvider (DocumentationProvider ... documentationProviders) {
    this(Arrays.asList(documentationProviders));
  }

  public CompositeDocumentationProvider(Collection<DocumentationProvider> providers) {
    myProviders = new ArrayList<DocumentationProvider>(providers);
  }

  public void inject (DocumentationProvider provider) {
    myProviders.add ( provider );
  }

  public void remove (DocumentationProvider provider) {
    myProviders.remove ( provider );
  }

  public List<DocumentationProvider> getProviders() {
    return Collections.unmodifiableList(myProviders);
  }

  public String getQuickNavigateInfo(PsiElement element) {
    for ( DocumentationProvider provider : myProviders ) {
      String result = provider.getQuickNavigateInfo(element);
      if ( result != null ) return result;
    }
    for (DocumentationProvider provider : Extensions.getExtensions(EP_NAME)) {
      final String result = provider.getQuickNavigateInfo(element);
      if (result != null) return result;
    }
    return null;
  }

  public List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
    for ( DocumentationProvider provider : myProviders ) {
      List<String> result = provider.getUrlFor(element,originalElement);
      if ( result != null ) return result;
    }
    for (DocumentationProvider provider : Extensions.getExtensions(EP_NAME)) {
      final List<String> result = provider.getUrlFor(element, originalElement);
      if (result != null) return result;
    }
    return null;
  }

  public String generateDoc(PsiElement element, PsiElement originalElement) {
    for ( DocumentationProvider provider : myProviders ) {
      String result = provider.generateDoc(element,originalElement);
      if ( result != null ) return result;
    }
    for (DocumentationProvider provider : Extensions.getExtensions(EP_NAME)) {
      final String result = provider.generateDoc(element, originalElement);
      if (result != null) return result;
    }
    return null;
  }

  public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
    for ( DocumentationProvider provider : myProviders ) {
      PsiElement result = provider.getDocumentationElementForLookupItem(psiManager,object,element);
      if ( result != null ) return result;
    }
    for (DocumentationProvider provider : Extensions.getExtensions(EP_NAME)) {
      final PsiElement result = provider.getDocumentationElementForLookupItem(psiManager, object, element);
      if (result != null) return result;
    }
    return null;
  }

  public PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {
    for ( DocumentationProvider provider : myProviders ) {
      PsiElement result = provider.getDocumentationElementForLink(psiManager,link,context);
      if ( result != null ) return result;
    }
    for (DocumentationProvider provider : Extensions.getExtensions(EP_NAME)) {
      final PsiElement result = provider.getDocumentationElementForLink(psiManager, link, context);
      if (result != null) return result;
    }
    return null;
  }


  @Nullable
  public CodeDocumentationProvider getFirstCodeDocumentationProvider() {
    for (DocumentationProvider provider : myProviders) {
      if (provider instanceof CodeDocumentationProvider) {
        return (CodeDocumentationProvider)provider;
      }
    }
    return null;
  }
}
