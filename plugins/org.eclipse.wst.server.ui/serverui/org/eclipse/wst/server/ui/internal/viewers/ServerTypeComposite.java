/**********************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *    IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.wst.server.ui.internal.viewers;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.ui.internal.ServerUIPlugin;
/**
 * 
 */
public class ServerTypeComposite extends AbstractTreeComposite {
	protected IServerType selection;
	protected ServerTypeSelectionListener listener;
	protected ServerTypeTreeContentProvider contentProvider;
	protected boolean initialSelection = true;
	
	public interface ServerTypeSelectionListener {
		public void serverTypeSelected(IServerType type);
	}
	
	public ServerTypeComposite(Composite parent, int style, ServerTypeSelectionListener listener2) {
		super(parent, style);
		this.listener = listener2;
	
		contentProvider = new ServerTypeTreeContentProvider(ServerTypeTreeContentProvider.STYLE_VENDOR);
		treeViewer.setContentProvider(contentProvider);
		treeViewer.setLabelProvider(new ServerTypeTreeLabelProvider());
		treeViewer.setInput(AbstractTreeContentProvider.ROOT);

		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				Object obj = getSelection(event.getSelection());
				if (obj instanceof IServerType) {
					selection = (IServerType) obj;
					setDescription(selection.getDescription());
				} else {
					selection = null;
					setDescription("");
				}
				listener.serverTypeSelected(selection);
			}
		});
		
		treeViewer.setSorter(new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 instanceof IServerType && !(e2 instanceof IServerType))
					return 1;
				if (!(e1 instanceof IServerType) && e2 instanceof IServerType)
					return -1;
				if (!(e1 instanceof IServerType && e2 instanceof IServerType))
					return super.compare(viewer, e1, e2);
				IServerType r1 = (IServerType) e1;
				IServerType r2 = (IServerType) e2;
				/*if (r1.getOrder() > r2.getOrder())
					return -1;
				else if (r1.getOrder() < r2.getOrder())
					return 1;
				else
					return super.compare(viewer, e1, e2);*/
				return r1.getName().compareTo(r2.getName());
			}
		});
	}
	
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible && initialSelection) {
			initialSelection = false;
			if (contentProvider.getInitialSelection() != null)
				treeViewer.setSelection(new StructuredSelection(contentProvider.getInitialSelection()), true);
		}
	}
	
	public void setHost(boolean localhost) {
		if (localhost == contentProvider.getHost())
			return;
		
		ISelection sel = treeViewer.getSelection();
		contentProvider.setHost(localhost);
		contentProvider.fillTree();
		treeViewer.refresh();
		//treeViewer.expandToLevel(2);
		treeViewer.setSelection(sel, true);
	}
	
	public void setIncludeTestEnvironments(boolean b) {
		contentProvider.setIncludeTestEnvironments(b);
		contentProvider.fillTree();
		treeViewer.refresh();
		//treeViewer.expandToLevel(2);
	}
	
	protected String getDescriptionLabel() {
		return null; //ServerUIPlugin.getResource("%serverTypeCompDescription");
	}

	protected String getTitleLabel() {
		return ServerUIPlugin.getResource("%serverTypeCompDescription");
	}

	protected String[] getComboOptions() {
		return new String[] { ServerUIPlugin.getResource("%name"),
			ServerUIPlugin.getResource("%vendor"), ServerUIPlugin.getResource("%version"),
			ServerUIPlugin.getResource("%moduleSupport") };
	}

	protected void viewOptionSelected(byte option) {
		ISelection sel = treeViewer.getSelection();
		treeViewer.setContentProvider(new ServerTypeTreeContentProvider(option));
		treeViewer.setSelection(sel);
	}

	public IServerType getSelectedServerType() {
		return selection;
	}
}