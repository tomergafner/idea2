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
package com.intellij.ide.plugins;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.ui.BooleanTableCellEditor;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.util.Function;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.SortableColumnModel;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: stathik
 * Date: Dec 26, 2003
 * Time: 3:51:58 PM
 * To change this template use Options | File Templates.
 */
public class InstalledPluginsTableModel extends PluginTableModel {
  public static Map<PluginId, Integer> NewVersions2Plugins = new HashMap<PluginId, Integer>();
  public static Set<PluginId> updatedPlugins = new HashSet<PluginId>();
  private final Map<PluginId, Boolean> myEnabled = new HashMap<PluginId, Boolean>();

  public InstalledPluginsTableModel(SortableProvider sortableProvider) {
    super.sortableProvider = sortableProvider;
    super.columns = new ColumnInfo[]{new EnabledPluginInfo(), new NameColumnInfo(), new BundledColumnInfo()};
    view = new ArrayList<IdeaPluginDescriptor>(Arrays.asList(PluginManager.getPlugins()));
    reset(view);

    for (Iterator<IdeaPluginDescriptor> iterator = view.iterator(); iterator.hasNext();) {
      @NonNls final String s = iterator.next().getPluginId().getIdString();
      if ("com.intellij".equals(s)) iterator.remove();
    }
    sortByColumn(getNameColumn());
  }

  public static int getCheckboxColumn() {
    return 0;
  }

  public int getNameColumn() {
    return 1;
  }

  public void addData(List<IdeaPluginDescriptor> list) {
    modifyData(list);
    reset(Arrays.asList(PluginManager.getPlugins()));
  }

  private void reset(final List<IdeaPluginDescriptor> list) {
    for (IdeaPluginDescriptor ideaPluginDescriptor : list) {
      if (ideaPluginDescriptor instanceof IdeaPluginDescriptorImpl) {
        myEnabled.put(ideaPluginDescriptor.getPluginId(), ((IdeaPluginDescriptorImpl)ideaPluginDescriptor).isEnabled());
      }
    }
  }

  public void modifyData(List<IdeaPluginDescriptor> list) {
    //  For each downloadable plugin we need to know whether its counterpart
    //  is already installed, and if yes compare the difference in versions:
    //  availability of newer versions will be indicated separately.
    for (IdeaPluginDescriptor descr : list) {
      PluginId descrId = descr.getPluginId();
      IdeaPluginDescriptor existing = PluginManager.getPlugin(descrId);
      if (existing != null) {
        if (descr instanceof PluginNode) {
          updateExistingPluginInfo(descr, existing);
        } else {
          view.add(descr);
          myEnabled.put(descr.getPluginId(), ((IdeaPluginDescriptorImpl)descr).isEnabled());
        }
      }
    }
    safeSort();
  }

  @Override
  public void filter(final ArrayList<IdeaPluginDescriptor> filtered) {
    view.clear();
    for (IdeaPluginDescriptor descriptor : filtered) {
      if (PluginManager.getPlugin(descriptor.getPluginId()) != null) {
        view.add(descriptor);
      }
    }
    super.filter(filtered);
  }

  private static void updateExistingPluginInfo(IdeaPluginDescriptor descr, IdeaPluginDescriptor existing) {
    int state = StringUtil.compareVersionNumbers(descr.getVersion(), existing.getVersion());
    if (state > 0 && !PluginManager.isIncompatible(descr)) {
      NewVersions2Plugins.put(existing.getPluginId(), 1);

      final IdeaPluginDescriptorImpl plugin = (IdeaPluginDescriptorImpl)existing;
      plugin.setDownloadsCount(descr.getDownloads());
      plugin.setVendor(descr.getVendor());
      plugin.setVendorEmail(descr.getVendorEmail());
      plugin.setVendorUrl(descr.getVendorUrl());
      plugin.setUrl(descr.getUrl());

    } else {
      if (NewVersions2Plugins.remove(existing.getPluginId()) != null) {
        updatedPlugins.add(existing.getPluginId());
      }
    }
  }

  public static boolean hasNewerVersion(PluginId descr) {
    return NewVersions2Plugins.containsKey(descr);
  }

  public static boolean wasUpdated(PluginId descr) {
    return updatedPlugins.contains(descr);
  }

  public boolean isEnabled(final PluginId pluginId) {
    final Boolean enabled = myEnabled.get(pluginId);
    return enabled != null && enabled.booleanValue();
  }

  public Map<PluginId, Boolean> getEnabledMap() {
    return myEnabled;
  }

  private class EnabledPluginInfo extends ColumnInfo<IdeaPluginDescriptorImpl, Boolean> {

    public EnabledPluginInfo() {
      super(IdeBundle.message("plugin.manager.enable.column.title"));
    }

    public Boolean valueOf(IdeaPluginDescriptorImpl ideaPluginDescriptor) {
      return myEnabled.get(ideaPluginDescriptor.getPluginId());
    }

    public boolean isCellEditable(final IdeaPluginDescriptorImpl ideaPluginDescriptor) {
      return true;
    }

    public Class getColumnClass() {
      return Boolean.class;
    }

    public TableCellEditor getEditor(final IdeaPluginDescriptorImpl o) {
      return new BooleanTableCellEditor();
    }

    public TableCellRenderer getRenderer(final IdeaPluginDescriptorImpl ideaPluginDescriptor) {
      return new BooleanTableCellRenderer();
    }

    public void setValue(final IdeaPluginDescriptorImpl ideaPluginDescriptor, final Boolean value) {
      myEnabled.put(ideaPluginDescriptor.getPluginId(), value);
      if (value.booleanValue()) {
        final Set<PluginId> deps = new HashSet<PluginId>();
        PluginManager.checkDependants(ideaPluginDescriptor, new Function<PluginId, IdeaPluginDescriptor>() {
            @Nullable
            public IdeaPluginDescriptor fun(final PluginId pluginId) {
              return PluginManager.getPlugin(pluginId);
            }
          }, new Condition<PluginId>() {
            public boolean value(final PluginId pluginId) {
              final Boolean enabled = myEnabled.get(pluginId);
              if (enabled == null) {
                return false;
              }
              if (!enabled.booleanValue()) {
                deps.add(pluginId);
              }
              return true;
            }
          });
        if (!deps.isEmpty()) {
          if (Messages.showOkCancelDialog("<html>The following plugins on which this plugin depends are disabled:<br>" + StringUtil.join(deps, new Function<PluginId, String>() {
            public String fun(final PluginId pluginId) {
              final IdeaPluginDescriptor pluginDescriptor = PluginManager.getPlugin(pluginId);
              assert pluginDescriptor != null;
              return pluginDescriptor.getName();
            }
          }, "<br>") + "<br>Would you like to enable them?</html>", "Enable Dependant Plugins", Messages.getQuestionIcon()) == DialogWrapper.OK_EXIT_CODE) {
            for (PluginId pluginId : deps) {
              myEnabled.put(pluginId, Boolean.TRUE);
            }
          }
        }
      }
    }

    public Comparator<IdeaPluginDescriptorImpl> getComparator() {
      final boolean sortDirection = (sortableProvider.getSortOrder() == SortableColumnModel.SORT_ASCENDING);
      return new Comparator<IdeaPluginDescriptorImpl>() {
        public int compare(final IdeaPluginDescriptorImpl o1, final IdeaPluginDescriptorImpl o2) {
          if (o1.isEnabled()) {
            if (o2.isEnabled()) {
              return 0;
            }
            return sortDirection ? -1 : 1;
          }
          else {
            if (!o2.isEnabled()) {
              return 0;
            }
            return sortDirection ? 1 : -1;
          }
        }
      };
    }
  }

  private static class BundledColumnInfo extends ColumnInfo<IdeaPluginDescriptor, Boolean> {
    public BundledColumnInfo() {
      super("Bundled");
    }

    @NotNull
    public Boolean valueOf(final IdeaPluginDescriptor ideaPluginDescriptor) {
      return ideaPluginDescriptor.isBundled();
    }

    @Override
    public Comparator<IdeaPluginDescriptor> getComparator() {
      return new Comparator<IdeaPluginDescriptor>() {
        public int compare(final IdeaPluginDescriptor o1, final IdeaPluginDescriptor o2) {
          return valueOf(o1).compareTo(valueOf(o2));
        }
      };
    }

    @Override
    public TableCellRenderer getRenderer(final IdeaPluginDescriptor ideaPluginDescriptor) {
      if (ideaPluginDescriptor.isBundled()) {
        return new BooleanTableCellRenderer() {
          @Override
          public Component getTableCellRendererComponent(final JTable table,
                                                         final Object value,
                                                         final boolean isSelected,
                                                         final boolean hasFocus,
                                                         final int row,
                                                         final int column) {
            final JCheckBox checkbox = (JCheckBox) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            checkbox.setEnabled(false);
            return checkbox;
          }
        };
      }
      return new DefaultTableCellRenderer() {
        protected void setValue(final Object value) {
          setText("");
        }
      };
    }
  }

  private class NameColumnInfo extends PluginManagerColumnInfo {
    public NameColumnInfo() {
      super(PluginManagerColumnInfo.COLUMN_NAME, sortableProvider);
    }

    public TableCellRenderer getRenderer(final IdeaPluginDescriptor ideaPluginDescriptor) {
      final DefaultTableCellRenderer cellRenderer = (DefaultTableCellRenderer)super.getRenderer(ideaPluginDescriptor);
      if (cellRenderer != null && ideaPluginDescriptor != null) {
        final IdeaPluginDescriptorImpl descriptor = (IdeaPluginDescriptorImpl)ideaPluginDescriptor;
        if (descriptor.isDeleted()) {
          cellRenderer.setIcon(IconLoader.getIcon("/actions/clean.png"));
        }
        else if (hasNewerVersion(ideaPluginDescriptor.getPluginId())) {
          cellRenderer.setIcon(IconLoader.getIcon("/nodes/pluginobsolete.png"));
        }
        else {
          cellRenderer.setIcon(IconLoader.getIcon("/nodes/plugin.png"));
        }
        final Boolean enabled = myEnabled.get(ideaPluginDescriptor.getPluginId());
        if (enabled != null && enabled.booleanValue()) {
          PluginManager.checkDependants(ideaPluginDescriptor, new Function<PluginId, IdeaPluginDescriptor>() {
            @Nullable
            public IdeaPluginDescriptor fun(final PluginId pluginId) {
              return PluginManager.getPlugin(pluginId);
            }
          }, new Condition<PluginId>() {
            public boolean value(final PluginId pluginId) {
              final Boolean enabled = myEnabled.get(pluginId);
              if (enabled == null || !enabled.booleanValue()) {
                cellRenderer.setForeground(Color.red);
                final IdeaPluginDescriptor plugin = PluginManager.getPlugin(pluginId);
                cellRenderer.setToolTipText(IdeBundle.message("plugin.manager.tooltip.warning", plugin != null ? plugin.getName() : pluginId.getIdString()));
                return false;
              }
              return true;
            }
          });
        }
        if (PluginManager.isIncompatible(ideaPluginDescriptor)) {
          cellRenderer.setToolTipText(IdeBundle.message("plugin.manager.incompatible.tooltip.warning", ApplicationNamesInfo.getInstance().getFullProductName()));
          cellRenderer.setForeground(Color.red);
        }
      }
      return cellRenderer;
    }
  }
}
