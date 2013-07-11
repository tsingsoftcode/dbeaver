/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.dbeaver.model.net.DBWNetworkHandler;
import org.jkiss.utils.CommonUtils;

/**
 * NetworkHandlerDescriptor
 */
public class NetworkHandlerDescriptor extends AbstractContextDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.networkHandler"; //$NON-NLS-1$

    private final String id;
    private final String label;
    private final String description;
    private DBWHandlerType type;
    private final boolean secured;
    private final String handlerClassName;
    private final String uiClassName;

    public NetworkHandlerDescriptor(
        IConfigurationElement config)
    {
        super(config.getContributor().getName(), config);
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.type = DBWHandlerType.valueOf(config.getAttribute(RegistryConstants.ATTR_TYPE).toUpperCase());
        this.secured = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_SECURED), false);
        this.handlerClassName = config.getAttribute(RegistryConstants.ATTR_HANDLER_CLASS);
        this.uiClassName = config.getAttribute(RegistryConstants.ATTR_UI_CLASS);
    }

    public String getId()
    {
        return id;
    }

    public String getLabel()
    {
        return label;
    }

    public String getDescription()
    {
        return description;
    }

    public DBWHandlerType getType()
    {
        return type;
    }

    public boolean isSecured()
    {
        return secured;
    }

    public <T extends DBWNetworkHandler> T createHandler(Class<T> handlerType)
        throws DBException
    {
        Class<T> toolClass = getObjectClass(handlerClassName, handlerType);
        if (toolClass == null) {
            throw new DBException("Handler class '" + toolClass + "' not found");
        }
        try {
            return toolClass.newInstance();
        }
        catch (Throwable ex) {
            throw new DBException("Can't create network handler '" + handlerClassName + "'", ex);
        }
    }

    public IObjectPropertyConfigurator<DBWHandlerConfiguration> createConfigurator()
        throws DBException
    {
        Class<? extends IObjectPropertyConfigurator> toolClass = getObjectClass(uiClassName, IObjectPropertyConfigurator.class);
        if (toolClass == null) {
            throw new DBException("Handler class '" + toolClass + "' not found");
        }
        try {
            return toolClass.newInstance();
        }
        catch (Throwable ex) {
            throw new DBException("Can't create network handler configurator '" + uiClassName + "'", ex);
        }
    }

}
