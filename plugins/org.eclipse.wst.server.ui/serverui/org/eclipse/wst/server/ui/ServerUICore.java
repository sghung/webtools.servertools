/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.server.ui;

import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.wst.server.ui.internal.ServerLabelProvider;
/**
 * Server UI core.
 * 
 * @since 1.0
 */
public class ServerUICore {
	private static ServerLabelProvider labelProvider;

	/**
	 * ServerUICore constructor comment.
	 */
	private ServerUICore() {
		super();
	}

	/**
	 * Returns a label provider that can be used for all server
	 * objects in the UI.
	 * 
	 * @return a label provider
	 */
	public static ILabelProvider getLabelProvider() {
		if (labelProvider == null)
			labelProvider = new ServerLabelProvider();
		return labelProvider;
	}
}