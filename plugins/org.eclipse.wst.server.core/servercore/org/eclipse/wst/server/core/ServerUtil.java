/**********************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial API and implementation
 **********************************************************************/
package org.eclipse.wst.server.core;

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.wst.server.core.internal.ServerPlugin;
import org.eclipse.wst.server.core.internal.Trace;
/**
 * Server utility methods.
 */
public class ServerUtil {
	/**
	 * Static class - cannot create an instance.
	 */
	private ServerUtil() {
		// do nothing
	}

	/**
	 * Returns true if the given configuration currently contains
	 * the given object.
	 *
	 * @param server org.eclipse.wst.server.core.IServer
	 * @param module org.eclipse.wst.server.core.IModule
	 * @return boolean
	 */
	public static boolean containsModule(IServer server, IModule module, IProgressMonitor monitor) {
		if (server == null)
			return false;
		Trace.trace(Trace.FINEST, "containsModule() " + server + " " + module);
		try {
			Iterator iterator = getAllContainedModules(server, monitor).iterator();
			while (iterator.hasNext()) {
				IModule module2 = (IModule) iterator.next();
				Trace.trace(Trace.FINEST, "module: " + module2 + " " + module.equals(module2));
				if (module.equals(module2))
					return true;
			}
		} catch (Throwable t) {
			// ignore
		}
		return false;
	}

	/**
	 * Returns all projects contained by the server. This included the
	 * projects that are in the configuration, as well as their
	 * children, and their children...
	 *
	 * @param server org.eclipse.wst.server.core.IServer
	 * @return java.util.List
	 */
	public static List getAllContainedModules(IServer server, IProgressMonitor monitor) {
		//Trace.trace("> getAllContainedModules: " + getName(configuration));
		List modules = new ArrayList();
		if (server == null)
			return modules;

		// get all of the directly contained projects
		IModule[] deploys = server.getModules(monitor);
		if (deploys == null || deploys.length == 0)
			return modules;

		int size = deploys.length;
		for (int i = 0; i < size; i++) {
			if (deploys[i] != null && !modules.contains(deploys[i]))
				modules.add(deploys[i]);
		}

		//Trace.trace("  getAllContainedModules: root level done");

		// get all of the module's children
		int count = 0;
		while (count < modules.size()) {
			IModule module = (IModule) modules.get(count);
			try {
				List childProjects = server.getChildModules(module, monitor);
				if (childProjects != null) {
					Iterator iterator = childProjects.iterator();
					while (iterator.hasNext()) {
						IModule child = (IModule) iterator.next();
						if (child != null && !modules.contains(child))
							modules.add(child);
					}
				}
			} catch (Exception e) {
				Trace.trace(Trace.SEVERE, "Error getting child modules for: " + module.getName(), e);
			}
			count ++;
		}

		//Trace.trace("< getAllContainedModules");

		return modules;
	}

	/**
	 * Returns a list of all servers that this module is configured on.
	 *
	 * @param module org.eclipse.wst.server.core.IModule
	 * @return java.util.List
	 */
	public static IServer[] getServersByModule(IModule module, IProgressMonitor monitor) {
		if (module == null)
			return new IServer[0];

		// do it the slow way - go through all servers and
		// see if this module is configured in it
		List list = new ArrayList();
		IServer[] servers = ServerCore.getResourceManager().getServers();
		if (servers != null) {
			int size = servers.length;
			for (int i = 0; i < size; i++) {
				if (containsModule(servers[i], module, monitor))
					list.add(servers[i]);
			}
		}
		
		IServer[] allServers = new IServer[list.size()];
		list.toArray(allServers);
		return allServers;
	}
	
	/**
	 * Returns a list of all servers that this module is configured on.
	 *
	 * @param module org.eclipse.wst.server.core.IModule
	 * @return java.util.List
	 */
	public static IServer[] getServersBySupportedModule(IModule module) {
		if (module == null)
			return new IServer[0];

		// do it the slow way - go through all servers and
		// see if this module is configured in it
		List list = new ArrayList();
		IServer[] servers = ServerCore.getResourceManager().getServers();
		if (servers != null) {
			int size = servers.length;
			for (int i = 0; i < size; i++) {
				if (isSupportedModule(servers[i].getServerType(), module.getModuleType()))
					list.add(servers[i]);
			}
		}
		
		IServer[] allServers = new IServer[list.size()];
		list.toArray(allServers);
		return allServers;
	}

	/**
	 * Returns a list of configurations that are supported by this
	 * server.
	 *
	 * @param server org.eclipse.wst.server.core.IServer
	 * @return java.util.List
	 */
	public static List getSupportedServerConfigurations(IServer server) {
		if (server == null)
			return new ArrayList();
	
		List list = new ArrayList();
	
		IServerConfiguration[] configs = ServerCore.getResourceManager().getServerConfigurations();
		if (configs != null) {
			int size = configs.length;
			for (int i = 0; i < size; i++) {
				//Trace.trace("Is supported configuration: " + getName(server) + " " + getName(configuration) + " " + server.isSupportedConfiguration(configuration));
				if (server.isSupportedConfiguration(configs[i]))
					list.add(configs[i]);
			}
		}
		return list;
	}

	/**
	 * Returns true if the given project is a server project.
	 *
	 * @param project org.eclipse.core.resources.IProject
	 * @return boolean
	 */
	public static boolean isServerProject(IProject project) {
		try {
			return project.hasNature(IServerProject.NATURE_ID);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Returns the project modules attached to a project.
	 */
	/*public static IProjectModule getModuleProject(IProject project) {
		if (project == null)
			return null;

		Iterator iterator = getModules(null, null, true).iterator();
		while (iterator.hasNext()) {
			IModule module = (IModule) iterator.next();
			if (module != null && module instanceof IProjectModule) {
				IProjectModule moduleProject = (IProjectModule) module;
				if (project.equals(moduleProject.getProject()))
					return moduleProject;
			}
		}
		return null;
	}*/
	
	/**
	 * Returns the project modules attached to a project.
	 */
	public static IModule[] getModules(IProject project) {
		if (project == null)
			return null;

		List list = new ArrayList();
		Iterator iterator = getModules(null, null, true).iterator();
		while (iterator.hasNext()) {
			IModule module = (IModule) iterator.next();
			if (module != null && project.equals(module.getProject()))
				list.add(module);
		}
		
		IModule[] modules = new IModule[list.size()];
		list.toArray(modules);
		return modules;
	}

	/**
	 * Returns a module from the given factoryId and memento.
	 * 
	 * @param java.lang.String factoryId
	 * @param java.lang.String memento
	 * @return org.eclipse.wst.server.core.IModule
	 */
	public static IModule getModule(String factoryId, String memento) {
		IModuleFactory[] moduleFactory = ServerCore.getModuleFactories();
		if (moduleFactory != null) {
			int size = moduleFactory.length;
			for (int i = 0; i < size; i++) {
				if (moduleFactory[i].getId().equals(factoryId)) {
					IModule module = moduleFactory[i].getModule(memento);
					if (module != null) {
						return module;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Returns true if the given class name s is a superclass or interface of
	 * the class obj.
	 * 
	 * @param c java.lang.Class
	 * @param s java.lang.String
	 * @return boolean
	 */
	protected static boolean isEnabled(Class c, String s) {
		if (s == null || s.length() == 0)
			return true;

		while (c != null && c != Object.class) {
			if (s.equals(c.getName()))
				return true;
			Class[] inf = c.getInterfaces();
			if (inf != null) {
				int size = inf.length;
				for (int i = 0; i < size; i++) {
					if (s.equals(inf[i].getName()))
						return true;
				}
			}
			c = c.getSuperclass();
		}
		return false;
	}
	
	/**
	 * Returns a module for the given object that may be launchable on
	 * a server using the Run on Server option. Throws an exception if it
	 * can't determine the object without possible plugin loading.
	 * 
	 * @param obj java.lang.Object
	 * @return IModule
	 * @throws java.lang.Exception
	 */
	public static IModule getModule(Object obj, boolean initialized) throws Exception {
		if (obj == null)
			return null;
		Trace.trace(Trace.FINEST, "ServerUtil.getModule()");
		IModuleObjectAdapter[] adapters = ServerCore.getModuleObjectAdapters();
		if (adapters != null) {
			int size = adapters.length;
			for (int i = 0; i < size; i++) {
				if (isEnabled(obj.getClass(), adapters[i].getObjectClassName())) {
					if (!initialized) {
						Trace.trace(Trace.FINEST, "getModule(): " + obj.getClass() + " " + adapters[i].getObjectClassName());
						throw new Exception();
					}
					IModuleObject moduleObject = adapters[i].getModuleObject(obj);
					if (moduleObject != null)
						return moduleObject.getModule();
				}
			}
		}
		return null;
	}

	/**
	 * Returns a list of the launchable clients for the given object.
	 *
	 * @return java.util.List
	 */
	public static List getModuleObjects(Object obj) {
		List list = new ArrayList();
		Trace.trace(Trace.FINEST, "ServerUtil.getModuleObjects()");
		IModuleObjectAdapter[] adapters = ServerCore.getModuleObjectAdapters();
		if (adapters != null) {
			int size = adapters.length;
			for (int i = 0; i < size; i++) {
				if (isEnabled(obj.getClass(), adapters[i].getObjectClassName())) {
					IModuleObject moduleObject = adapters[i].getModuleObject(obj);
					Trace.trace(Trace.FINEST, "moduleObject: " + moduleObject);
					if (moduleObject != null)
						list.add(moduleObject);
				}
			}
		}
		return list;
	}

	/**
	 * Returns the first launchable object for the given server and module
	 * object.
	 * 
	 * @param server
	 * @param moduleObject
	 * @return ILaunchable
	 */
	public static ILaunchable getLaunchable(IServer server, IModuleObject moduleObject) {
		ILaunchableAdapter[] adapters = ServerCore.getLaunchableAdapters();
		if (adapters != null) {
			int size = adapters.length;
			for (int i = 0; i < size; i++) {
				try {
					ILaunchable launchable = adapters[i].getLaunchable(server, moduleObject);
					Trace.trace(Trace.FINEST, "adapter= " + adapters[i] + ", launchable= " + launchable);
					if (launchable != null)
						return launchable;
				} catch (Exception e) {
					Trace.trace(Trace.SEVERE, "Error in launchable adapter", e);
				}
			}
		}
		return null;
	}

	/**
	 * Returns the launchable clients for the given server and launchable
	 * object.
	 *
	 * @param server org.eclipse.wst.server.core.IServer
	 * @param moduleObject org.eclipse.wst.server.core.IModuleObject
	 * @param launchMode String
	 * @return java.util.List
	 */
	public static List getLaunchableClients(IServer server, ILaunchable launchable, String launchMode) {
		ArrayList list = new ArrayList();
		IClient[] clients = ServerCore.getClients();
		if (clients != null) {
			int size = clients.length;
			for (int i = 0; i < size; i++) {
				Trace.trace(Trace.FINEST, "client= " + clients[i]);
				if (clients[i].supports(server, launchable, launchMode))
					list.add(clients[i]);
			}
		}
		return list;
	}

	/**
	 * Returns the first launchable object for the given server and module
	 * object.
	 * 
	 * @param server
	 * @param moduleObject
	 * @return ILaunchable
	 */
	public static ILaunchable getLaunchable(IServer server, List moduleObjects) {
		Iterator iterator = moduleObjects.iterator();
		while (iterator.hasNext()) {
			IModuleObject moduleObject = (IModuleObject) iterator.next();
			ILaunchable launchable = getLaunchable(server, moduleObject);
			if (launchable != null)
				return launchable;
		}
		return null;
	}

	/**
	 * Returns the factory that created the given module.
	 *
	 * @param org.eclipse.wst.server.core.IModule
	 * @return org.eclipse.wst.server.core.IModuleFactory
	 */
	/*public static IModuleFactory getModuleFactory(IModule module) {
		String id = module.getFactoryId();
		if (id == null)
			return null;

		IModuleFactory[] factories = ServerCore.getModuleFactories();
		if (factories != null) {
			int size = factories.length;
			for (int i = 0; i < size; i++) {
				if (id.equals(factories[i].getId()))
					return factories[i];
			}
		}
		return null;
	}*/

	/**
	 * Return all the available modules from all factories whose
	 * type includes the given id.
	 * 
	 * @param type java.lang.String
	 * @param onlyProjectModules boolean
	 * @return java.util.List
	 */
	public static List getModules(String type, String version, boolean onlyProjectModules) {
		List list = new ArrayList();

		IModuleFactory[] factories = ServerCore.getModuleFactories();
		if (factories != null) {
			int size = factories.length;
			for (int i = 0; i < size; i++) {
				//if (!(onlyProjectModules && factory.isProjectModuleFactory())) {
				if (isSupportedModule(factories[i].getModuleTypes(), type, version)) {
					IModule[] modules = factories[i].getModules();
					if (modules != null) {
						int size2 = modules.length;
						for (int j = 0; j < size2; j++)
							list.add(modules[j]);
					}
				}
				//}
			}
		}
		return list;
	}
	
	public static boolean isSupportedModule(IServerType serverType, IModuleType2 moduleType) {
		IRuntimeType runtimeType = serverType.getRuntimeType();
		return isSupportedModule(runtimeType.getModuleTypes(), moduleType.getId(), moduleType.getVersion());
	}
	
	public static boolean isSupportedModule(IModuleType2[] moduleTypes, String type, String version) {
		if (moduleTypes != null) {
			int size = moduleTypes.length;
			for (int i = 0; i < size; i++) {
				if (isSupportedModule(moduleTypes[i], type, version))
					return true;
			}
		}
		return false;
	}
	
	public static boolean isSupportedModule(IModuleType2 moduleType, String type, String version) {
		String type2 = moduleType.getId();
		if (matches(type, type2)) {
			String version2 = moduleType.getVersion();
			if (matches(version, version2))
				return true;
		}
		return false;
	}

	protected static boolean matches(String a, String b) {
		if (a == null || b == null || "*".equals(a) || "*".equals(b) || a.startsWith(b) || b.startsWith(a))
			return true;
		return false;
	}

	/**
	 * Return all the available modules from all factories.
	 * 
	 * @return java.util.List
	 */
	public static List getModules() {
		List list = new ArrayList();
		
		IModuleFactory[] factories = ServerCore.getModuleFactories();
		if (factories != null) {
			int size = factories.length;
			for (int i = 0; i < size; i++) {
				IModule[] modules = factories[i].getModules();
				if (modules != null) {
					int size2 = modules.length;
					for (int j = 0; j < size2; j++) {
						if (!list.contains(modules[i]))
							list.add(modules[i]);
					}
				}
			}
		}
		return list;
	}

	/**
	 * Adds or removes modules from a server. Will search for the first parent module
	 * of each module and add it to the server instead. This method will handle multiple
	 * modules having the same parent (the parent will only be added once), but may not
	 * handle the case where the same module or parent is being both added and removed.
	 * 
	 * @param server
	 * @param add
	 * @param remove
	 * @param monitor
	 * @throws CoreException
	 */
	public static void modifyModules(IServerWorkingCopy server, IModule[] add, IModule[] remove, IProgressMonitor monitor) throws CoreException {
		if (add == null)
			add = new IModule[0];
		if (remove == null)
			remove = new IModule[0];
		
		int size = add.length;
		List addParentModules = new ArrayList();
		for (int i = 0; i < size; i++) {
			boolean found = false;
			try {
				List parents = server.getParentModules(add[i], monitor);
				if (parents != null) {
					found = true;
					if (parents.size() > 0) {				
						Object parent = parents.get(0);
						found = true;
						if (!addParentModules.contains(parent))
							addParentModules.add(parent);
					}
				} 
			} catch (Exception e) {
				Trace.trace(Trace.WARNING, "Could not find parent module", e);
			}
			
			if (!found)
				addParentModules.add(add[i]);
		}
		
		size = remove.length;
		List removeParentModules = new ArrayList();
		for (int i = 0; i < size; i++) {
			boolean found = false;
			try {
				List parents = server.getParentModules(remove[i], monitor);
				if (parents != null) {
					found = true;
					if (parents.size() > 0) {				
						Object parent = parents.get(0);
						found = true;
						if (!removeParentModules.contains(parent))
							removeParentModules.add(parent);
					}
				} 
			} catch (Exception e) {
				Trace.trace(Trace.WARNING, "Could not find parent module 2", e);
			}
			
			if (!found)
				removeParentModules.add(remove[i]);
		}
		
		IModule[] add2 = new IModule[addParentModules.size()];
		addParentModules.toArray(add2);
		IModule[] remove2 = new IModule[removeParentModules.size()];
		removeParentModules.toArray(remove2);
		
		server.modifyModules(add2, remove2, monitor);
	}

	/**
	 * Returns true if the given server is already started in the given
	 * mode, or could be (re)started in the start mode.
	 * 
	 * @param server org.eclipse.wst.server.core.IServer
	 * @param launchMode java.lang.String
	 * @return boolean
	 */
	public static boolean isCompatibleWithLaunchMode(IServer server, String launchMode) {
		if (server == null || launchMode == null)
			return false;

		int state = server.getServerState();
		if (state == IServer.STATE_STARTED && launchMode.equals(server.getMode()))
			return true;

		if (server.getServerType().supportsLaunchMode(launchMode))
			return true;
		return false;
	}

	/**
	 * Filters the servers to those that are already started in the given launchMode
	 * or can be (re)started in the given launchMode.
	 * 
	 * @param server org.eclipse.wst.server.core.IServer[]
	 * @param launchMode java.lang.String
	 * @return org.eclipse.wst.server.core.IServer[]
	 */
	public static IServer[] filterServersByLaunchMode(IServer[] servers, String launchMode) {
		if (servers == null || servers.length == 0)
			return servers;
		
		int size = servers.length;
		List list = new ArrayList();
		for (int i = 0; i < size; i++) {
			if (isCompatibleWithLaunchMode(servers[i], launchMode))
				list.add(servers[i]);
		}
		IServer[] temp = new IServer[list.size()];
		list.toArray(temp);
		return temp;
	}
	
	/**
	 * Visit all the modules in the server configuration.
	 */
	public static void visit(IServer server, IModuleVisitor visitor, IProgressMonitor monitor) {
		if (server == null)
			return;
		
		IModule[] modules = server.getModules(monitor);
		if (modules != null) { 
			int size = modules.length;
			for (int i = 0; i < size; i++) {
				if (!visitModule(server, new ArrayList(), modules[i], visitor, monitor))
					return;
			}
		}
	}

	/**
	 * Returns true to keep visiting, and false to stop.
	 */
	private static boolean visitModule(IServer server, List parents, IModule module, IModuleVisitor visitor, IProgressMonitor monitor) {
		if (server == null || module == null || parents == null)
			return true;
		
		if (!visitor.visit(parents, module))
			return false;
		
		List children = server.getChildModules(module, monitor);
		if (children != null) {
			Iterator iterator = children.iterator();
			while (iterator.hasNext()) {
				IModule module2 = (IModule) iterator.next();
				
				List parents2 = new ArrayList(parents.size() + 1);
				parents2.addAll(parents);
				parents2.add(module);
				
				if (!visitModule(server, parents2, module2, visitor, monitor))
					return false;
			}
		}
			
		return true;
	}

	public static boolean isNameInUse(IRuntime runtime) {
		IRuntime[] runtimes = ServerCore.getResourceManager().getRuntimes();
		if (runtimes != null) {
			int size = runtimes.length;
			for (int i = 0; i < size; i++) {
				if (!runtime.equals(runtimes[i]) && runtime.getName().equals(runtimes[i].getName())) {
					if (!runtime.isWorkingCopy() || !runtimes[i].equals(((IRuntimeWorkingCopy)runtime).getOriginal()))
						return true;
				}
			}
		}
		return false;
	}

	public static void setRuntimeDefaultName(IRuntimeWorkingCopy wc) {
		String typeName = wc.getRuntimeType().getName();
		
		String name = ServerPlugin.getResource("%defaultRuntimeName", new String[] {typeName});
		int i = 2;
		while (isNameInUse(name)) {
			name = ServerPlugin.getResource("%defaultRuntimeName2", new String[] {typeName, i + ""});
			i++;
		}
		wc.setName(name);
	}

	public static void setServerDefaultName(IServerWorkingCopy wc) {
		String typeName = wc.getServerType().getName();
		String host = wc.getHost();
		
		String name = ServerPlugin.getResource("%defaultServerName", new String[] {typeName, host});
		int i = 2;
		while (isNameInUse(name)) {
			name = ServerPlugin.getResource("%defaultServerName2", new String[] {typeName, host, i + ""});
			i++;
		}
		wc.setName(name);
	}

	public static void setServerConfigurationDefaultName(IServerConfigurationWorkingCopy wc) {
		String typeName = wc.getServerConfigurationType().getName();
		
		String name = ServerPlugin.getResource("%defaultServerConfigurationName", new String[] {typeName});
		int i = 2;
		while (isNameInUse(name)) {
			name = ServerPlugin.getResource("%defaultServerConfigurationName2", new String[] {typeName, i + ""});
			i++;
		}
		wc.setName(name);
	}
	
	public static IProject getDefaultServerProject() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		if (projects != null) {
			int size = projects.length;
			for (int i = 0; i < size; i++) {
				if (ServerUtil.isServerProject(projects[i]))
					return projects[i];
			}
		}
		return ResourcesPlugin.getWorkspace().getRoot().getProject(findUnusedServerProjectName());
	}

	private static boolean isValidFilename(String name) {
		IStatus status = ResourcesPlugin.getWorkspace().validateName(name, IResource.FILE);
		if (status != null && !status.isOK())
			return false;
		
		status = ResourcesPlugin.getWorkspace().validateName(name, IResource.FOLDER);
		if (status != null && !status.isOK())
			return false;
		
		return true;
	}

	private static String getValidFileName(String name) {
		if (isValidFilename(name))
			return name;
	
		// remove invalid characters
		String[] s = new String[] {".", "\\", "/", "?", ":", "*", "\"", "|", "<", ">"};
		int ind = 0;
		while (ind < s.length) {
			int index = name.indexOf(s[ind]);
			while (index >= 0) {
				name = name.substring(0, index) + name.substring(index+1);
				index = name.indexOf(s[ind]);
			}
			ind++;
		}
		return name;
	}

	public static IFile getUnusedServerFile(IProject project, IServerType type) {
		String typeName = getValidFileName(type.getName());
		String name = ServerPlugin.getResource("%defaultServerName3", new String[] {typeName})+ "."  + IServer.FILE_EXTENSION;
		int i = 2;
		while (isFileNameInUse(project, name)) {
			name = ServerPlugin.getResource("%defaultServerName4", new String[] {typeName, i + ""}) + "."  + IServer.FILE_EXTENSION;
			i++;
		}
		return project.getFile(name);
	}

	public static IFile getUnusedServerConfigurationFile(IProject project, IServerConfigurationType type) {
		String typeName = getValidFileName(type.getName());
		String name = ServerPlugin.getResource("%defaultServerConfigurationName", new String[] {typeName}) + "."  + IServerConfiguration.FILE_EXTENSION;
		int i = 2;
		while (isFileNameInUse(project, name)) {
			name = ServerPlugin.getResource("%defaultServerConfigurationName2", new String[] {typeName, i + ""}) + "."  + IServerConfiguration.FILE_EXTENSION;
			i++;
		}
		return project.getFile(name);
	}

	/**
	 * Returns true if an element exists with the given name.
	 *
	 * @param name java.lang.String
	 * @return boolean
	 */
	private static boolean isNameInUse(String name) {
		if (name == null)
			return true;
	
		IResourceManager rm = ServerCore.getResourceManager();
		List list = new ArrayList();
		
		addAll(list, rm.getRuntimes());
		addAll(list, rm.getServers());
		addAll(list, rm.getServerConfigurations());

		Iterator iterator = list.iterator();
		while (iterator.hasNext()) {
			IElement element = (IElement) iterator.next();
			if (name.equalsIgnoreCase(element.getName()))
				return true;
		}

		return false;
	}
	
	private static void addAll(List list, Object[] obj) {
		if (obj == null)
			return;
		
		int size = obj.length;
		for (int i = 0; i < size; i++) {
			list.add(obj[i]);
		}
	}
	
	/**
	 * Returns true if an element exists with the given name.
	 *
	 * @param name java.lang.String
	 * @return boolean
	 */
	private static boolean isFileNameInUse(IProject project, String name) {
		if (name == null || project == null)
			return false;
		
		if (project.getFile(name).exists())
			return true;
		if (project.getFolder(name).exists())
			return true;
	
		return false;
	}

	/**
	 * Finds an unused project name to use as a server project.
	 * 
	 * @return java.lang.String
	 */
	private static String findUnusedServerProjectName() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		String name = ServerPlugin.getResource("%defaultServerProjectName", "");
		int count = 1;
		while (root.getProject(name).exists()) {
			name = ServerPlugin.getResource("%defaultServerProjectName", ++count + "");
		}
		return name;
	}
	
	/**
	 * Sort the given list of IOrdered items into indexed order. This method
	 * modifies the original list, but returns the value for convenience.
	 *
	 * @param list java.util.List
	 * @return java.util.List
	 */
	public static List sortOrderedList(List list) {
		if (list == null)
			return null;

		int size = list.size();
		for (int i = 0; i < size - 1; i++) {
			for (int j = i + 1; j < size; j++) {
				IOrdered a = (IOrdered) list.get(i);
				IOrdered b = (IOrdered) list.get(j);
				if (a.getOrder() > b.getOrder()) {
					Object temp = a;
					list.set(i, b);
					list.set(j, temp);
				}
			}
		}
		return list;
	}
	
	/**
	 * Sort the given list of IOrdered items into reverse indexed order. This method
	 * modifies the original list, but returns the value for convenience.
	 *
	 * @param list java.util.List
	 * @return java.util.List
	 */
	public static List sortOrderedListReverse(List list) {
		if (list == null)
			return null;

		int size = list.size();
		for (int i = 0; i < size - 1; i++) {
			for (int j = i + 1; j < size; j++) {
				IOrdered a = (IOrdered) list.get(i);
				IOrdered b = (IOrdered) list.get(j);
				if (a.getOrder() < b.getOrder()) {
					Object temp = a;
					list.set(i, b);
					list.set(j, temp);
				}
			}
		}
		return list;
	}

	/**
	 * Return a list of all runtime targets that match the given type and version.
	 * If type or version is null, it matches all of that type or version.
	 * 
	 * @param type
	 * @param version
	 * @return 
	 */
	public static List getRuntimes(String type, String version) {
		List list = new ArrayList();
		IRuntime[] runtimes = ServerCore.getResourceManager().getRuntimes();
		if (runtimes != null) {
			int size = runtimes.length;
			for (int i = 0; i < size; i++) {
				IRuntimeType runtimeType = runtimes[i].getRuntimeType();
				if (runtimeType != null && isSupportedModule(runtimeType.getModuleTypes(), type, version)) {
					list.add(runtimes[i]);
				}
			}
		}
		return list;
	}

	/**
	 * Return a list of all runtime types that match the given type and version.
	 * If type or version is null, it matches all of that type or version.
	 * 
	 * @param type
	 * @param version
	 * @return 
	 */
	public static List getRuntimeTypes(String type, String version) {
		List list = new ArrayList();
		IRuntimeType[] runtimeTypes = ServerCore.getRuntimeTypes();
		if (runtimeTypes != null) {
			int size = runtimeTypes.length;
			for (int i = 0; i < size; i++) {
				if (isSupportedModule(runtimeTypes[i].getModuleTypes(), type, version)) {
					list.add(runtimeTypes[i]);
				}
			}
		}
		return list;
	}
	
	/**
	 * Return a list of all runtime types that match the given type, version,
	 * and partial runtime type id. If type, version, or runtimeTypeId is null,
	 * it matches all of that type or version.
	 * 
	 * @param type
	 * @param version
	 * @return 
	 */
	public static List getRuntimeTypes(String type, String version, String runtimeTypeId) {
		List list = new ArrayList();
		IRuntimeType[] runtimeTypes = ServerCore.getRuntimeTypes();
		if (runtimeTypes != null) {
			int size = runtimeTypes.length;
			for (int i = 0; i < size; i++) {
				if (isSupportedModule(runtimeTypes[i].getModuleTypes(), type, version)) {
					if (runtimeTypeId == null || runtimeTypes[i].getId().startsWith(runtimeTypeId))
						list.add(runtimeTypes[i]);
				}
			}
		}
		return list;
	}

	/**
	 * Returns a list of all servers that this deployable is not currently
	 * configured on, but could be added to. If includeErrors is true, this
	 * method return servers where the parent deployable may throw errors. For
	 * instance, this deployable may be the wrong spec level.
	 *
	 * @param module com.ibm.etools.server.core.IModule
	 * @return com.ibm.etools.server.core.IServer[]
	 */
	public static IServer[] getAvailableServersForModule(IModule module, boolean includeErrors, IProgressMonitor monitor) {
		if (module == null)
			return new IServer[0];

		// do it the slow way - go through all servers and
		// see if this deployable is not configured in it
		// but could be added
		List list = new ArrayList();
		IServer[] servers = ServerCore.getResourceManager().getServers();
		if (servers != null) {
			int size = servers.length;
			for (int i = 0; i < size; i++) {
				if (!containsModule(servers[i], module, monitor)) {
					try {
						List parents = servers[i].getParentModules(module, monitor);
						if (parents != null && !parents.isEmpty()) {
							Iterator iterator2 = parents.iterator();
							boolean found = false;
							while (!found && iterator2.hasNext()) {
								IModule parent = (IModule) iterator2.next();
								IStatus status = servers[i].canModifyModules(new IModule[] { parent }, new IModule[0], monitor);
								if (status == null || status.isOK()){
									list.add(servers[i]);
									found = true;
								}
							}
						}
					} catch (Exception se) {
						if (includeErrors)
							list.add(servers[i]);
					}
				}
			}
		}
		
		// make sure that the preferred server is the first one
		/*IServer server = ServerCore.getServerPreferences().getDeployableServerPreference(deployable);
		if (server != null && list.contains(server) && list.indexOf(server) != 0) {
			list.remove(server);
			list.add(0, server);
		}*/

		IServer[] allServers = new IServer[list.size()];
		list.toArray(allServers);
		return allServers;
	}

	/*public static boolean isDefaultAvailable(IServerType serverType, IModuleType2 moduleType) {
		if (!isSupportedModule(serverType, moduleType))
			return false;
	
		// remote servers won't automatically work (i.e. without hostname)
		if (!serverType.supportsLocalhost())
			return false;
		
		// if it needs a runtime, check if there is one
		if (serverType.hasRuntime()) {
			IRuntime[] runtimes = ServerCore.getResourceManager().getRuntimes(serverType.getRuntimeType());
			if (runtimes == null || runtimes.length == 0)
				return false;
		}
		return true;
	}*/
}