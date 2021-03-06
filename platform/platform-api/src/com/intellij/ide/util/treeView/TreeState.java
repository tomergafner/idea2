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
package com.intellij.ide.util.treeView;

import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.util.*;
import com.intellij.util.ui.tree.TreeUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

public class TreeState implements JDOMExternalizable {
  @NonNls private static final String PATH = "PATH";
  @NonNls private static final String PATH_ELEMENT = "PATH_ELEMENT";
  @NonNls private static final String USER_OBJECT = "USER_OBJECT";

  static class PathElement implements JDOMExternalizable {
    public String myItemId;
    public String myItemType;

    private final int myItemIndex;
    private Object myUserObject;

    public PathElement(final String itemId, final String itemType, final int itemIndex, Object userObject) {
      myItemId = itemId;
      myItemType = itemType;

      myItemIndex = itemIndex;
      myUserObject = userObject;
    }

    public PathElement() {
      myItemIndex = -1;
      myUserObject = null;
    }

    public boolean matchedWith(NodeDescriptor nodeDescriptor) {
      return Comparing.equal(myItemId, getDescriptorKey(nodeDescriptor)) &&
             Comparing.equal(myItemType, getDescriptorType(nodeDescriptor));
    }

    public boolean matchedWithByObject(Object object) {
      return myUserObject != null && myUserObject.equals(object);
    }

    public void readExternal(Element element) throws InvalidDataException {
      DefaultJDOMExternalizer.readExternal(this, element);
      myUserObject = element.getAttributeValue(USER_OBJECT);
    }

    public void writeExternal(Element element) throws WriteExternalException {
      DefaultJDOMExternalizer.writeExternal(this, element);
      if (myUserObject instanceof String){
        element.setAttribute(USER_OBJECT, (String)myUserObject);
      }
    }
  }

  private final List<List<PathElement>> myExpandedPaths;
  private final List<List<PathElement>> mySelectedPaths;

  private TreeState(List<List<PathElement>> expandedPaths, final List<List<PathElement>> selectedPaths) {
    myExpandedPaths = expandedPaths;
    mySelectedPaths = selectedPaths;
  }

  public TreeState() {
    this(new ArrayList<List<PathElement>>(), new ArrayList<List<PathElement>>());
  }

  public void readExternal(Element element) throws InvalidDataException {
    myExpandedPaths.clear();
    final List paths = element.getChildren(PATH);
    for (final Object path : paths) {
      Element xmlPathElement = (Element)path;
      myExpandedPaths.add(readPath(xmlPathElement));
    }
  }

  private static List<PathElement> readPath(final Element xmlPathElement) throws InvalidDataException {
    final ArrayList<PathElement> result = new ArrayList<PathElement>();
    final List elements = xmlPathElement.getChildren(PATH_ELEMENT);
    for (final Object element : elements) {
      Element xmlPathElementElement = (Element)element;
      final PathElement pathElement = new PathElement();
      pathElement.readExternal(xmlPathElementElement);
      result.add(pathElement);
    }
    return result;
  }

  public static TreeState createOn(JTree tree, final DefaultMutableTreeNode treeNode) {
    return new TreeState(createExpandedPaths(tree, treeNode), createSelectedPaths(tree, treeNode));
  }

  public void writeExternal(Element element) throws WriteExternalException {
    for (List<PathElement> path : myExpandedPaths) {
      final Element pathElement = new Element(PATH);
      writeExternal(pathElement, path);
      element.addContent(pathElement);
    }
  }

  private static void writeExternal(final Element pathXmlElement, final List<PathElement> path) throws WriteExternalException {
    for (final PathElement aPath : path) {
      final Element pathXmlElementElement = new Element(PATH_ELEMENT);
      aPath.writeExternal(pathXmlElementElement);
      pathXmlElement.addContent(pathXmlElementElement);
    }
  }

  public static TreeState createOn(@NotNull JTree tree) {
    return new TreeState(createPaths(tree), new ArrayList<List<PathElement>>());
  }


  private static List<List<PathElement>> createPaths(final JTree tree) {
    final ArrayList<List<PathElement>> result = new ArrayList<List<PathElement>>();
    final List<TreePath> expandedPaths = TreeUtil.collectExpandedPaths(tree);
    for (final TreePath expandedPath : expandedPaths) {
      final List<PathElement> path = createPath(expandedPath);
      if (path != null) {
        result.add(path);
      }
    }
    return result;
  }

  private static List<List<PathElement>> createExpandedPaths(JTree tree, final DefaultMutableTreeNode treeNode) {
    final ArrayList<List<PathElement>> result = new ArrayList<List<PathElement>>();
    final List<TreePath> expandedPaths = TreeUtil.collectExpandedPaths(tree, new TreePath(treeNode.getPath()));
    for (final TreePath expandedPath : expandedPaths) {
      final List<PathElement> path = createPath(expandedPath);
      if (path != null) {
        result.add(path);
      }
    }
    return result;
  }

  private static List<List<PathElement>> createSelectedPaths(JTree tree, final DefaultMutableTreeNode treeNode) {
    final ArrayList<List<PathElement>> result = new ArrayList<List<PathElement>>();
    final List<TreePath> selectedPaths
      = TreeUtil.collectSelectedPaths(tree, new TreePath(treeNode.getPath()));
    for (final TreePath expandedPath : selectedPaths) {
      final List<PathElement> path = createPath(expandedPath);
      if (path != null) {
        result.add(path);
      }
    }
    return result;
  }

  private static List<PathElement> createPath(final TreePath treePath) {
    final ArrayList<PathElement> result = new ArrayList<PathElement>();
    for (int i = 0; i < treePath.getPathCount(); i++) {
      final Object pathComponent = treePath.getPathComponent(i);
      if (pathComponent instanceof DefaultMutableTreeNode) {
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode)pathComponent;
        final TreeNode parent = node.getParent();

        final Object userObject = node.getUserObject();
        if (userObject instanceof NodeDescriptor) {
          final NodeDescriptor nodeDescriptor = (NodeDescriptor)userObject;
          //nodeDescriptor.update();
          final int childIndex = parent != null ? parent.getIndex(node) : 0;
          result.add(new PathElement(getDescriptorKey(nodeDescriptor), getDescriptorType(nodeDescriptor), childIndex, nodeDescriptor));
        }
        else {
          result.add(new PathElement("", "", 0, userObject));
        }
      }
      else {
        return null;
      }
    }
    return result;
  }

  private static String getDescriptorKey(final NodeDescriptor nodeDescriptor) {
    if (nodeDescriptor instanceof AbstractTreeNode) {
      Object value;
      if (nodeDescriptor instanceof NodeDescriptorProvidingKey) {
        value = ((NodeDescriptorProvidingKey)nodeDescriptor).getKey();
      }
      else {
        value = ((AbstractTreeNode)nodeDescriptor).getValue();
      }

      if (value instanceof NavigationItem) {
        try {
          final String name = ((NavigationItem)value).getName();
          return name != null ? name : value.toString();
        }
        catch (Exception e) {
          //ignore for invalid psi element
        }
      }
    }
    return nodeDescriptor.toString();
  }

  private static String getDescriptorType(final NodeDescriptor nodeDescriptor) {
    return nodeDescriptor.getClass().getName();
  }

  public void applyTo(JTree tree) {
    applyExpanded(getFacade(tree), tree.getModel().getRoot());
  }

  private void applyExpanded(final TreeFacade tree, final Object root) {
    tree.getIntialized().doWhenDone(new Runnable() {
      public void run() {
        _applyExpanded(tree, root);
      }
    });
  }

  private void _applyExpanded(TreeFacade tree, Object root) {
    if (!(root instanceof DefaultMutableTreeNode)) {
      return;
    }
    final DefaultMutableTreeNode nodeRoot = (DefaultMutableTreeNode)root;
    final TreeNode[] nodePath = nodeRoot.getPath();
    if (nodePath.length > 0) {
      for (final List<PathElement> path : myExpandedPaths) {
        applyTo(nodePath.length - 1,path, root, tree);
      }
    }
  }

  public void applyTo(final JTree tree, final DefaultMutableTreeNode node) {
    applyExpanded(getFacade(tree), node);
    if (tree.getSelectionCount() == 0) {
      applySelected(tree, node);
    }
  }

  private void applySelected(final JTree tree, final DefaultMutableTreeNode node) {
    TreeUtil.unselect(tree, node);
    List<TreePath> selectionPaths = new ArrayList<TreePath>();
    for (List<PathElement> pathElements : mySelectedPaths) {
      applySelectedTo(pathElements, tree.getModel().getRoot(), tree, selectionPaths);
    }

    if (selectionPaths.size() > 1) {
      for (TreePath path : selectionPaths) {
        tree.addSelectionPath(path);
      }
    }
  }


  private static DefaultMutableTreeNode findMatchedChild(DefaultMutableTreeNode parent, PathElement pathElement) {

    for (int j = 0; j < parent.getChildCount(); j++) {
      final TreeNode child = parent.getChildAt(j);
      if (!(child instanceof DefaultMutableTreeNode)) continue;
      final DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)child;
      final Object userObject = childNode.getUserObject();
      if (pathElement.matchedWithByObject(userObject)) return childNode;
    }

    for (int j = 0; j < parent.getChildCount(); j++) {
      final TreeNode child = parent.getChildAt(j);
      if (!(child instanceof DefaultMutableTreeNode)) continue;
      final DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)child;
      final Object userObject = childNode.getUserObject();
      if (!(userObject instanceof NodeDescriptor)) continue;
      final NodeDescriptor nodeDescriptor = (NodeDescriptor)userObject;
      if (pathElement.matchedWith(nodeDescriptor)) return childNode;
    }

    if (parent.getChildCount() > 0) {
      int index = pathElement.myItemIndex;
      if (index >= parent.getChildCount()) {
        index = parent.getChildCount()-1;
      }
      final TreeNode child = parent.getChildAt(index);
      if (child instanceof DefaultMutableTreeNode) {
        return (DefaultMutableTreeNode) child;
      }
    }

    return null;

  }

  private static boolean applyTo(final int positionInPath, final List<PathElement> path, final Object root, final TreeFacade tree) {
    if (!(root instanceof DefaultMutableTreeNode)) return false;

    final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)root;

    final Object userObject = treeNode.getUserObject();
    final PathElement pathElement = path.get(positionInPath);

    if (userObject instanceof NodeDescriptor) {
      if (!pathElement.matchedWith((NodeDescriptor)userObject)) return false;
    }
    else {
      if (!pathElement.matchedWithByObject(userObject)) return false;
    }

    tree.expand(treeNode).doWhenDone(new Runnable() {
      public void run() {
        if (positionInPath == path.size() - 1) {
          return;
        }

        for (int j = 0; j < treeNode.getChildCount(); j++) {
          final TreeNode child = treeNode.getChildAt(j);
          final boolean resultFromChild = applyTo(positionInPath + 1, path, child, tree);
          if (resultFromChild) {
            break;
          }
        }
      }
    });


    return true;
  }

  private static void applySelectedTo(final List<PathElement> path,
                                      Object root,
                                      JTree tree,
                                      final List<TreePath> outSelectionPaths) {

    for (int i = 1; i < path.size(); i++) {
      if (!(root instanceof DefaultMutableTreeNode)) return;

      root = findMatchedChild((DefaultMutableTreeNode)root, path.get(i));
    }

    if (!(root instanceof DefaultMutableTreeNode)) return;

    final TreePath pathInNewTree = new TreePath(((DefaultMutableTreeNode) root).getPath());
    TreeUtil.selectPath(tree, pathInNewTree);
    outSelectionPaths.add(pathInNewTree);
  }

  interface TreeFacade {
    ActionCallback getIntialized();
    ActionCallback expand(DefaultMutableTreeNode node);
  }

  private static TreeFacade getFacade(JTree tree) {
    final AbstractTreeBuilder builder = AbstractTreeBuilder.getBuilderFor(tree);
    return builder != null ? new BuilderFacade(builder) : new JTreeFacade(tree);
  }

  static class JTreeFacade implements TreeFacade {

    private JTree myTree;

    JTreeFacade(JTree tree) {
      myTree = tree;
    }

    public ActionCallback expand(DefaultMutableTreeNode node) {
      myTree.expandPath(new TreePath(node.getPath()));
      return new ActionCallback.Done();
    }

    public ActionCallback getIntialized() {
      return new ActionCallback.Done();
    }
  }

  static class BuilderFacade implements TreeFacade {

    private AbstractTreeBuilder myBuilder;

    BuilderFacade(AbstractTreeBuilder builder) {
      myBuilder = builder;
    }

    public ActionCallback getIntialized() {
      return myBuilder.getIntialized();
    }

    public ActionCallback expand(DefaultMutableTreeNode node) {
      final Object userObject = node.getUserObject();
      if (!(userObject instanceof NodeDescriptor)) return new ActionCallback.Rejected();

      NodeDescriptor desc = (NodeDescriptor)userObject;

      final Object element = myBuilder.getTreeStructureElement(desc);

      final ActionCallback result = new ActionCallback();

      myBuilder.expand(element, new Runnable() {
        public void run() {
          result.setDone();
        }
      });

      return result;
    }
  }

}

