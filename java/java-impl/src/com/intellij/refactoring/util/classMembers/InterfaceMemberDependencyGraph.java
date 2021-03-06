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
package com.intellij.refactoring.util.classMembers;

import com.intellij.psi.*;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.classMembers.MemberDependencyGraph;
import com.intellij.util.containers.HashMap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class InterfaceMemberDependencyGraph implements MemberDependencyGraph<PsiMember, MemberInfo> {
  protected HashSet<PsiMethod> myInterfaceDependencies = null;
  protected HashMap<PsiMethod,HashSet<PsiClass>> myMembersToInterfacesMap = new HashMap<PsiMethod, HashSet<PsiClass>>();
  protected HashSet<PsiClass> myImplementedInterfaces;
  protected HashMap<PsiClass,HashSet<PsiMethod>> myMethodsFromInterfaces;
  protected PsiClass myClass;

  public InterfaceMemberDependencyGraph(PsiClass aClass) {
    myClass = aClass;
    myImplementedInterfaces = new HashSet<PsiClass>();
    myMethodsFromInterfaces = new com.intellij.util.containers.HashMap<PsiClass, HashSet<PsiMethod>>();
  }

  public void memberChanged(MemberInfo memberInfo) {
    if (ClassMembersUtil.isImplementedInterface(memberInfo)) {
      final PsiClass aClass = (PsiClass) memberInfo.getMember();
      myInterfaceDependencies = null;
      myMembersToInterfacesMap = null;
      if(memberInfo.isChecked()) {
        myImplementedInterfaces.add(aClass);
      }
      else {
        myImplementedInterfaces.remove(aClass);
      }
    }
  }

  public Set<? extends PsiMember> getDependent() {
    if(myInterfaceDependencies == null) {
      myInterfaceDependencies = new HashSet<PsiMethod>();
      myMembersToInterfacesMap = new com.intellij.util.containers.HashMap<PsiMethod, HashSet<PsiClass>>();
      for (final PsiClass implementedInterface : myImplementedInterfaces) {
        addInterfaceDeps(implementedInterface);
      }
    }
    return myInterfaceDependencies;
  }

  public Set<? extends PsiMember> getDependenciesOf(PsiMember member) {
    final Set dependent = getDependent();
    if(dependent.contains(member)) return myMembersToInterfacesMap.get(member);
    return null;
  }

  public String getElementTooltip(PsiMember member) {
    final Set<? extends PsiMember> dependencies = getDependenciesOf(member);
    if(dependencies == null || dependencies.size() == 0) return null;
    StringBuffer buffer = new StringBuffer();
    buffer.append(RefactoringBundle.message("interface.member.dependency.required.by.interfaces", dependencies.size()));

    for (Iterator<? extends PsiMember> iterator = dependencies.iterator(); iterator.hasNext();) {
      PsiClass aClass = (PsiClass) iterator.next();
      buffer.append(aClass.getName());
      if(iterator.hasNext()) {
        buffer.append(", ");
      }
    }
    return buffer.toString();
  }

  protected void addInterfaceDeps(PsiClass intf) {
    HashSet<PsiMethod> interfaceMethods = myMethodsFromInterfaces.get(intf);

    if(interfaceMethods == null) {
      interfaceMethods = new HashSet<PsiMethod>();
      buildInterfaceMethods(interfaceMethods, intf);
      myMethodsFromInterfaces.put(intf, interfaceMethods);
    }
    for (PsiMethod method : interfaceMethods) {
      HashSet<PsiClass> interfaces = myMembersToInterfacesMap.get(method);
      if (interfaces == null) {
        interfaces = new HashSet<PsiClass>();
        myMembersToInterfacesMap.put(method, interfaces);
      }
      interfaces.add(intf);
    }
    myInterfaceDependencies.addAll(interfaceMethods);
  }

  private void buildInterfaceMethods(HashSet<PsiMethod> interfaceMethods, PsiClass intf) {
    PsiMethod[] methods = intf.getMethods();
    for (PsiMethod method1 : methods) {
      PsiMethod method = myClass.findMethodBySignature(method1, true);
      if (method != null) {
        interfaceMethods.add(method);
      }
    }

    PsiReferenceList implementsList = intf.getImplementsList();
    if (implementsList != null) {
      PsiClassType[] implemented = implementsList.getReferencedTypes();
      for (PsiClassType aImplemented : implemented) {
        PsiClass resolved = aImplemented.resolve();
        if (resolved != null) {
          buildInterfaceMethods(interfaceMethods, resolved);
        }
      }
    }

    PsiReferenceList extendsList = intf.getExtendsList();
    if (extendsList != null) {
      PsiClassType[] extended = extendsList.getReferencedTypes();
      for (PsiClassType aExtended : extended) {
        PsiClass ref = aExtended.resolve();
        if (ref != null) {
          buildInterfaceMethods(interfaceMethods, ref);
        }
      }
    }
  }

}
