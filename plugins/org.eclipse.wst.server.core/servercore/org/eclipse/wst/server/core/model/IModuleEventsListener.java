/**********************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial API and implementation
 **********************************************************************/
package org.eclipse.wst.server.core.model;
/**
 * 
 */
public interface IModuleEventsListener {
	/**
	 * A module or factory has been changed as specified in the event.
	 *
	 * @param factoryEvent org.eclipse.wst.server.core.model.IModuleFactoryEvent[]
	 * @param event org.eclipse.wst.server.core.model.IModuleEvent[]
	 */
	public void moduleEvents(IModuleFactoryEvent[] factoryEvent, IModuleEvent[] event);
}
