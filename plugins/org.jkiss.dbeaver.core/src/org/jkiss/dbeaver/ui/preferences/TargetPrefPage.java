/*
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.preferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.SelectDataSourceDialog;

/**
 * TargetPrefPage
 */
public abstract class TargetPrefPage extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    static final Log log = LogFactory.getLog(TargetPrefPage.class);

    private DBNDataSource containerNode;
    private Composite parentComposite;
    private Button dataSourceSettingsButton;
    private Control configurationBlockControl;
    private Link changeSettingsTargetLink;
    private ControlEnableState blockEnableState;

    protected TargetPrefPage()
    {
    }

    public final boolean isDataSourcePreferencePage()
    {
        return containerNode != null;
    }

    protected abstract boolean hasDataSourceSpecificOptions(DataSourceDescriptor project);

    protected abstract boolean supportsDataSourceSpecificOptions();

    protected void createPreferenceHeader(Composite composite)
    {
    }

    protected abstract Control createPreferenceContent(Composite composite);

    protected abstract void loadPreferences(IPreferenceStore store);
    protected abstract void savePreferences(IPreferenceStore store);
    protected abstract void clearPreferences(IPreferenceStore store);

    protected abstract String getPropertyPageID();

    public DataSourceDescriptor getDataSourceContainer()
    {
        return containerNode.getObject();
    }

    @Override
    public void init(IWorkbench workbench)
    {
    }

    @Override
    public IAdaptable getElement()
    {
        return containerNode;
    }

    @Override
    public void setElement(IAdaptable element)
    {
        if (element == null) {
            return;
        }
        containerNode = (DBNDataSource) element.getAdapter(DBNDataSource.class);
        if (containerNode == null) {
            IDatabaseEditorInput dbInput = (IDatabaseEditorInput) element.getAdapter(IDatabaseEditorInput.class);
            if (dbInput != null) {
                DBNNode dbNode = dbInput.getTreeNode();
                if (dbNode instanceof DBNDataSource) {
                    containerNode = (DBNDataSource)dbNode;
                }
            } else if (element instanceof IDataSourceProvider) {
                DBPDataSource dataSource = ((IDataSourceProvider) element).getDataSource();
                if (dataSource != null) {
                    containerNode = (DBNDataSource) DBeaverCore.getInstance().getNavigatorModel().findNode(dataSource.getContainer());
                }
            } else if (element instanceof DBSDataSourceContainer) {
                containerNode = (DBNDataSource) DBeaverCore.getInstance().getNavigatorModel().findNode((DBSDataSourceContainer) element);
            }
        }
    }

    @Override
    protected Label createDescriptionLabel(Composite parent)
    {
        parentComposite = parent;
        if (isDataSourcePreferencePage()) {
            Composite composite = UIUtils.createPlaceholder(parent, 2);
            composite.setFont(parent.getFont());
            composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

            dataSourceSettingsButton = new Button(composite, SWT.CHECK);
            dataSourceSettingsButton.addSelectionListener(new SelectionAdapter()
            {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    boolean enabled = dataSourceSettingsButton.getSelection();
                    enableDataSourceSpecificSettings(enabled);
                }
            });
            String dataSourceName = containerNode.getDataSourceContainer().getName();
            dataSourceSettingsButton.setText(NLS.bind(CoreMessages.pref_page_target_button_use_datasource_settings, dataSourceName));
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            dataSourceSettingsButton.setLayoutData(gd);

            changeSettingsTargetLink = createLink(composite, CoreMessages.pref_page_target_link_show_global_settings);
            changeSettingsTargetLink.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

        } else if (supportsDataSourceSpecificOptions()) {
            changeSettingsTargetLink = createLink(
                parent,
                CoreMessages.pref_page_target_link_show_datasource_settings);
            changeSettingsTargetLink.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
        }

        Label horizontalLine = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
        horizontalLine.setFont(parent.getFont());

        createPreferenceHeader(parent);

        return super.createDescriptionLabel(parent);
    }

    /*
     * @see org.eclipse.jface.preference.IPreferencePage#createContents(Composite)
     */
    @Override
    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        configurationBlockControl = createPreferenceContent(composite);
        configurationBlockControl.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        if (isDataSourcePreferencePage()) {
            boolean useProjectSettings = hasDataSourceSpecificOptions(getDataSourceContainer());
            enableDataSourceSpecificSettings(useProjectSettings);
        }

        {
            IPreferenceStore store = useDataSourceSettings() ?
                getDataSourceContainer().getPreferenceStore() :
                DBeaverCore.getGlobalPreferenceStore();
            loadPreferences(store);
        }

        Dialog.applyDialogFont(composite);
        return composite;
    }

    private Link createLink(Composite composite, String text)
    {
        Link link = new Link(composite, SWT.NONE);
        link.setFont(composite.getFont());
        link.setText("<A>" + text + "</A>");  //$NON-NLS-1$//$NON-NLS-2$
        link.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                doLinkActivated((Link) e.widget);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
                widgetSelected(e);
            }
        });
        return link;
    }

    protected void enableDataSourceSpecificSettings(boolean useProjectSpecificSettings)
    {
        dataSourceSettingsButton.setSelection(useProjectSpecificSettings);
        enablePreferenceContent(useProjectSpecificSettings);
        updateLinkVisibility();
        doStatusChanged();
    }

    protected void doStatusChanged()
    {
/*
        if (!isProjectPreferencePage() || useDataSourceSettings()) {
            updateStatus(fBlockStatus);
        } else {
            updateStatus(new StatusInfo());
        }
*/
    }

    protected void enablePreferenceContent(boolean enable)
    {
        if (enable) {
            if (blockEnableState != null) {
                blockEnableState.restore();
                blockEnableState = null;
            }
        } else {
            if (blockEnableState == null) {
                blockEnableState = ControlEnableState.disable(configurationBlockControl);
            }
        }
    }

    protected boolean useDataSourceSettings()
    {
        return isDataSourcePreferencePage() && dataSourceSettingsButton != null && dataSourceSettingsButton.getSelection();
    }

    private void updateLinkVisibility()
    {
        if (changeSettingsTargetLink == null || changeSettingsTargetLink.isDisposed()) {
            return;
        }

        if (isDataSourcePreferencePage()) {
            changeSettingsTargetLink.setEnabled(!useDataSourceSettings());
        }
    }

    private void doLinkActivated(Link link)
    {
        PreferenceDialog prefDialog = null;
        if (isDataSourcePreferencePage()) {
            // Show global settings
            prefDialog = PreferencesUtil.createPreferenceDialogOn(
                getShell(),
                getPropertyPageID(),
                null,//new String[]{getPropertyPageID()},
                null);
        } else if (supportsDataSourceSpecificOptions()) {
            // Select datasource
            DataSourceDescriptor dataSource = SelectDataSourceDialog.selectDataSource(getShell());
            if (dataSource != null) {
                DBNNode dsNode = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(dataSource);
                if (dsNode instanceof DBNDataSource) {
                    prefDialog = PreferencesUtil.createPropertyDialogOn(
                        getShell(),
                        (DBNDataSource)dsNode,
                        getPropertyPageID(),
                        null,//new String[]{getPropertyPageID()},
                        null);
                }
            }
        }
        if (prefDialog != null) {

            prefDialog.open();
        }
    }

    @Override
    protected final void performApply()
    {
        performOk();
    }

    @Override
    public final boolean performOk()
    {
        IPreferenceStore store = isDataSourcePreferencePage() ?
            getDataSourceContainer().getPreferenceStore() :
            DBeaverCore.getGlobalPreferenceStore();
        if (isDataSourcePreferencePage() && !useDataSourceSettings()) {
            // Just delete datasource specific settings
            clearPreferences(store);
        } else {
            savePreferences(store);
        }
        RuntimeUtils.savePreferenceStore(store);
        return super.performOk();
    }

}
