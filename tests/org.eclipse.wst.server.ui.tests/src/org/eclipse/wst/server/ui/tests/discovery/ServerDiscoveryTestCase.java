/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.server.ui.tests.discovery;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.discovery.internal.model.Extension;
import org.eclipse.wst.server.discovery.internal.model.ExtensionUpdateSite;
import org.eclipse.wst.server.ui.tests.TestsPlugin;
import org.eclipse.wst.server.ui.tests.internal.util.ZipUtil;

public class ServerDiscoveryTestCase extends TestCase {
	
	/*
	 * Tests the server adapter discovery.
	 * 
	 * Note: the test repositories do not actually install properly. They are there purely for the detection. The actual
	 * install of real server adapters is not possible because it requires the restart of Eclipse after for verifications.
	 */
	
	public static IPath metadataPath = TestsPlugin.getDefault().getStateLocation();
	protected static final String resourcesPathName = "resources"; //$NON-NLS-1$
	protected static final String updateSiteServerAdapterWithSiteXML = "ServerAdapterWithSiteXML"; //$NON-NLS-1$
	protected static final String updateSiteServerAdapterWithServerAdapterProperty = "ServerAdapterWithServerAdapterProperty"; //$NON-NLS-1$
	protected static final String updateSiteServerAdapterWithP2GeneratedFromCategoryXMLFeature = "ServerAdapterWithP2GeneratedFromCategoryXMLFeature"; //$NON-NLS-1$
	protected static final String updateSiteInvalid = "ServerAdapterMissingAnyServerAdapterDiscovery"; //$NON-NLS-1$
	protected static final String zipExtension = ".zip"; //$NON-NLS-1$
	protected static final String serverAdapterSiteName = "serverAdapterSites.xml"; //$NON-NLS-1$
	
	// A helper method for retrieving the extensions
	protected List<Extension> getExtensions(File filePath){
		try {
			String finalPath = filePath.toString();
			String os = System.getProperty("os.name"); //$NON-NLS-1$
			
			// Building the URI for Windows does not work using the File.toURI().toURL().
			// Windows needs to be handled with a special case by appending the "file:///"
			if (os != null && os.toUpperCase().indexOf("WINDOWS") >= 0){ //$NON-NLS-1$
				finalPath = finalPath.replaceAll("\\\\", "/");  //$NON-NLS-1$ //$NON-NLS-2$
				finalPath = "file:///" + finalPath; //$NON-NLS-1$
			}
			else {
				finalPath = filePath.toURI().toURL().toString();
			}
			
			ExtensionUpdateSite extensionUpdateSite = new ExtensionUpdateSite(finalPath, null, null);

			List<Extension> foundExtension = extensionUpdateSite.getExtensions(new NullProgressMonitor());
			return foundExtension;
		}
		catch (Exception e){
			// Print stack trace for diagnostics
			e.printStackTrace();
		}
		return null;
	}
	
	public void testServerAdapterWithSiteXML() throws CoreException{
		ZipUtil.copyArchiveToMetadataDir(resourcesPathName + File.separator + updateSiteServerAdapterWithSiteXML + zipExtension);
		File file = new File(metadataPath + File.separator + updateSiteServerAdapterWithSiteXML);
		assertTrue("Update site does not exist",file.exists()); //$NON-NLS-1$
		
		List<Extension> extensionList = getExtensions(file);
		
		assertNotNull("Extension list cannot be null",extensionList); //$NON-NLS-1$
		
		assertTrue("Failed to find the expected server adapater",!extensionList.isEmpty()); //$NON-NLS-1$
		Extension e = extensionList.get(0);
		assertNotNull("Extension found should not be null", e); //$NON-NLS-1$
		assertTrue("Failed to find expected server adapter's name. Found : " + e.getName(), ("ServerAdapterWithSiteXMLFeature".equals(e.getName()))); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void testServerAdapterWithServerAdapterProperty(){
		ZipUtil.copyArchiveToMetadataDir(resourcesPathName + File.separator + updateSiteServerAdapterWithServerAdapterProperty + zipExtension);
		File file = new File(metadataPath + File.separator + updateSiteServerAdapterWithServerAdapterProperty);
		assertTrue("Update site does not exist",file.exists()); //$NON-NLS-1$
		
		List<Extension> extensionList = getExtensions(file);		
		assertNotNull("Extension list cannot be null",extensionList); //$NON-NLS-1$		
		assertTrue("Failed to find the expected server adapater",!extensionList.isEmpty()); //$NON-NLS-1$
		Extension e = extensionList.get(0);
		assertNotNull("Extension found should not be null", e); //$NON-NLS-1$
		assertTrue("Failed to find expected server adapter's name. Found : " + e.getName(), ("ServerAdapterWithServerAdapterProperty".equals(e.getName()))); //$NON-NLS-1$ //$NON-NLS-2$	
	}	
	
	public void testServerAdapterWithP2GeneratedFromCategoryXMLFeature(){
		ZipUtil.copyArchiveToMetadataDir(resourcesPathName + File.separator + updateSiteServerAdapterWithP2GeneratedFromCategoryXMLFeature + zipExtension);
		File file = new File(metadataPath + File.separator + updateSiteServerAdapterWithP2GeneratedFromCategoryXMLFeature);
		assertTrue("Update site does not exist",file.exists()); //$NON-NLS-1$
		
		List<Extension> extensionList = getExtensions(file);		
		assertNotNull("Extension list cannot be null",extensionList); //$NON-NLS-1$		
		assertTrue("Failed to find the expected server adapater",!extensionList.isEmpty()); //$NON-NLS-1$
		Extension e = extensionList.get(0);
		assertNotNull("Extension found should not be null", e); //$NON-NLS-1$
		assertTrue("Failed to find expected server adapter's name. Found : " + e.getName() , ("ServerAdapterWithP2GeneratedFromCategoryXMLFeature".equals(e.getName()))); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void testServerAdapterMissingAnyServerAdapterDiscovery(){
		// We expect no Extension to be returned
		ZipUtil.copyArchiveToMetadataDir(resourcesPathName + File.separator + updateSiteInvalid + zipExtension);
		File file = new File(metadataPath + File.separator + updateSiteInvalid);
		assertTrue("Update site does not exist",file.exists()); //$NON-NLS-1$
		
		List<Extension> extensionList = getExtensions(file);		
		assertNotNull("Extension list cannot be null",extensionList); //$NON-NLS-1$		
		assertTrue("No extension should be found since update site is invalid",extensionList.isEmpty()); //$NON-NLS-1$		
	}
}
