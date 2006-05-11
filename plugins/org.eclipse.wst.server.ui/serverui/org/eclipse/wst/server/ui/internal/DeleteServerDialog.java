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
package org.eclipse.wst.server.ui.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServer.IOperationListener;
/**
 * Dialog that prompts a user to delete server(s) and/or server configuration(s).
 */
public class DeleteServerDialog extends Dialog {
	protected IServer[] servers;
	protected IFolder[] configs;

	protected List runningServersList;

	protected Button checkDeleteConfigs;
	protected Button checkDeleteRunning;
	protected Button checkDeleteRunningStop;

	/**
	 * DeleteServerDialog constructor comment.
	 * 
	 * @param parentShell a shell
	 * @param servers an array of servers
	 * @param configs an array of server configurations
	 */
	public DeleteServerDialog(Shell parentShell, IServer[] servers, IFolder[] configs) {
		super(parentShell);
		
		if (servers == null || configs == null)
			throw new IllegalArgumentException();
		
		this.servers = servers;
		this.configs = configs;
		
		runningServersList = new ArrayList();
		for (int i = 0 ; i < servers.length ; ++i) {
			if (servers[i].getServerState() != IServer.STATE_STOPPED)
				runningServersList.add(servers[i]);
		}

		setBlockOnOpen(true);
	}

	/**
	 *
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.deleteServerDialogTitle);
	}

	/**
	 * 
	 */
	protected Control createDialogArea(Composite parent) {
		// create a composite with standard margins and spacing
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setFont(parent.getFont());
		//WorkbenchHelp.setHelp(composite, ContextIds.TERMINATE_SERVER_DIALOG);
	
		Label label = new Label(composite, SWT.NONE);
		if (servers.length == 1)
			label.setText(NLS.bind(Messages.deleteServerDialogMessage, servers[0].getName()));
		else
			label.setText(NLS.bind(Messages.deleteServerDialogMessageMany, servers.length + ""));
		//label.setLayoutData(new GridData());
		
		if (configs.length > 0) {
			checkDeleteConfigs = new Button(composite, SWT.CHECK);
			checkDeleteConfigs.setText(NLS.bind(Messages.deleteServerDialogLooseConfigurations, configs[0].getName()));
			checkDeleteConfigs.setSelection(true);
		}
		
		// prompt for stopping running servers
		int size = runningServersList.size();
		if (size > 0) {
			checkDeleteRunning = new Button(composite, SWT.CHECK);
			checkDeleteRunning.setText(NLS.bind(Messages.deleteServerDialogRunningServer, ((IServer)runningServersList.get(0)).getName()));
			checkDeleteRunning.setSelection(true);
			
			checkDeleteRunningStop = new Button(composite, SWT.CHECK);
			checkDeleteRunningStop.setText(NLS.bind(Messages.deleteServerDialogRunningServerStop, ((IServer)runningServersList.get(0)).getName()));
			checkDeleteRunningStop.setSelection(true);
			GridData data = new GridData();
			data.horizontalIndent = 15;
			checkDeleteRunningStop.setLayoutData(data);
			
			checkDeleteRunning.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					checkDeleteRunningStop.setEnabled(checkDeleteRunning.getSelection());
				}
			});
		}
		
		Dialog.applyDialogFont(composite);
		
		return composite;
	}

	protected void okPressed() {
		final boolean checked = (checkDeleteConfigs != null && checkDeleteConfigs.getSelection());
		final boolean deleteRunning = (checkDeleteRunning != null && checkDeleteRunning.getSelection());
		final boolean deleteRunningStop = (checkDeleteRunningStop != null && checkDeleteRunningStop.getSelection());
		
		if (runningServersList.size() > 0) {
			// stop servers and/or updates servers' list
			prepareForDeletion(deleteRunning, deleteRunningStop);
			//monitor.worked(1);
		}
		
		try {
			WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
				protected void execute(IProgressMonitor monitor) throws CoreException {
					// since stopping can be long, let's animate progessDialog
					monitor.beginTask(Messages.deleteServerTask, 2);
					
					if (servers.length == 0) {
						// all servers have been deleted from list
						return;
					}
					try {
						int size = servers.length;
						for (int i = 0; i < size; i++) {
							servers[i].delete();
						}
						
						if (checked) {
							size = configs.length;
							for (int i = 0; i < size; i++) {
								configs[i].delete(true, true, monitor);
							}
						}
					} catch (Exception e) {
						Trace.trace(Trace.SEVERE, "Error while deleting resources", e);
					}
				}
			};
			new ProgressMonitorDialog(getShell()).run(true, true, op);
		} catch (Exception e) {
			Trace.trace(Trace.SEVERE, "Error deleting resources", e);
		}
		
		super.okPressed();
	}

	/**
	 * Updates servers' & configs' lists. If <code>deleteRunning</code> is <code>true</code>
	 * 	and a server can't be stopped, it isn't removed.
	 * @param deleteRunning if <code>true</code> running servers will be stopped
	 * 	before being deleted, if <code>false</code> running servers will be removed
	 *    from deletion list.
	 */
	protected void prepareForDeletion(boolean deleteRunning, boolean stopRunning) {
		// converts servers & configs to list to facilitate removal
		List serversList = new LinkedList(Arrays.asList(servers));
		List configsList = new LinkedList(Arrays.asList(configs));
		if (deleteRunning == false) {
			// don't delete servers or configurations
			int size = runningServersList.size();
			for (int i = 0; i < size; i++) {
				IServer server = (IServer) runningServersList.get(i);
				serversList.remove(server);
				if (server.getServerConfiguration() != null)
					configsList.remove(server.getServerConfiguration());
			}
		} else {
			if (stopRunning) {
				// stop running servers and wait for them (stop is asynchronous)
				IServer s;
				MultiServerStopListener listener = new MultiServerStopListener();
				int expected = 0;
				Iterator iter = runningServersList.iterator();
				while (iter.hasNext()) {
					s = (IServer) iter.next();
					if (s.canStop().isOK()) {
						++expected;
						s.stop(false, listener);
					} else {
						// server can't be stopped, don't delete it
						serversList.remove(s);
						configsList.remove(s.getServerConfiguration());
					}
				}
				try {
					while (expected != listener.getNumberStopped()) {
						Thread.sleep(100);
					}
				} catch (InterruptedException e) {
					Trace.trace(Trace.WARNING, "Interrupted while waiting for servers stop");
				}
			}
		}
		servers = new IServer[serversList.size()];
		serversList.toArray(servers);
		configs = new IFolder[configsList.size()];
		configsList.toArray(configs);
	}

	/**
	 * Class used to wait all servers stop. Use one instance
	 * for a group of servers and loop to see if the number stopped
	 * equals the number of servers waiting to stop.
	 */
	class MultiServerStopListener implements IOperationListener {
		protected int num; 

		public void done(IStatus result) {
			num++;
		}

		public int getNumberStopped() {
			return num;
		}
	}
}