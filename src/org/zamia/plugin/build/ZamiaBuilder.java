/*
 * Copyright 2007-2010 by the authors indicated in the @author tags.
 * All rights reserved.
 *
 * See the LICENSE file for details.
 *
 */

package org.zamia.plugin.build;

import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.PlatformUI;
import org.zamia.BuildPath;
import org.zamia.BuildPathEntry;
import org.zamia.ERManager;
import org.zamia.ExceptionLogger;
import org.zamia.SourceFile;
import org.zamia.ZamiaLogger;
import org.zamia.ZamiaProject;
import org.zamia.ZamiaProjectBuilder;
import org.zamia.plugin.ZamiaPlugin;
import org.zamia.plugin.ZamiaProjectMap;
import org.zamia.plugin.editors.ZamiaEditor;
import org.zamia.plugin.efs.ZamiaFileSystem;
import org.zamia.plugin.ui.XilinxPrjImportWizard;
import org.zamia.plugin.views.navigator.ZamiaNavigator;
import org.zamia.util.HashSetArray;
import org.zamia.util.Native;


/**
 * 
 * @author Guenter Bartsch
 * 
 */

public class ZamiaBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = "org.zamia.plugin.zamiaBuilder";

	private static final ZamiaLogger logger = ZamiaLogger.getInstance();

	private static final ExceptionLogger el = ExceptionLogger.getInstance();

	public static final QualifiedName BUILDPATH_QN = new QualifiedName(ZamiaPlugin.PLUGIN_ID, "buildpath");
	
	private boolean fNeedFullBuild = false;

	private HashSetArray<SourceFile> fChangedSFs;

	private boolean fBPChanged = false;
	
	/**Used when build path is deleted*/
	private static SourceFile fakeBp = new SourceFile(new File("fake://BuildPath.txt"), "fake://BuildPath.txt"); 

	private static boolean fAutoBuildEnabled = true;

	private static long fDisabledUntil;

	public static String getPersistentBuildPath(IProject project) {
		try {
			String result = project.getPersistentProperty(ZamiaBuilder.BUILDPATH_QN);
			if (result != null)
				return result;
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return fakeBp.getLocalPath();
	}
	
	class DeltaBuildDetector implements IResourceDeltaVisitor {

		public boolean visit(IResourceDelta aDelta) throws CoreException {
			//Eclipse bug - you cannot overwrite linked resource
			//https://bugs.eclipse.org/bugs/show_bug.cgi?id=366339
				
			IResource resource = aDelta.getResource();
			
			if (resource instanceof IFile) {
				
				IFile file = (IFile) resource;
				String name = file.getName();

				if ((aDelta.getKind() & (IResourceDelta.ADDED | IResourceDelta.CHANGED)) != 0 
						&& name.toLowerCase().endsWith(".prj")) {
					XilinxPrjImportWizard.importXilinxPrj(getProject(), file.getLocation().toOSString());
					return false;
				}
				
//				file.getProjectRelativePath()); // -- alias.vhdl
//				file.getFullPath()); 		// -- /proj1/alias.vhdl (fullpath in Eclipse view)
//				file.getRawLocation()); 	// -- PARENT-2-PROJECT_LOC/workspace/zamiacad - Copy/examples/alias/alias.vhdl (resource was linked using PROJECT_LOC)
//				file.getLocation());		// -- C:/Users/valentin/workspace/zamiacad - Copy/examples/alias/alias.vhdl (seems true absolute path)
//				file.getLocationURI()); 	// -- file:/C:/Users/valentin/workspace/zamiacad%20-%20Copy/examples/alias/alias.vhdl
				
				ZamiaProject zProj = ZamiaProjectMap.getZamiaProject(getProject()); 
				SourceFile sf = ZamiaPlugin.getSourceFile(file);
				
				((ZamiaProjectMap.EclipseProjectFileIterator)(zProj.fBasePath)).listChanged(aDelta, sf);
						
				
				BuildPath bp = zProj.getBuildPath();
				final boolean isBp;
						
				switch (aDelta.getKind()) {
				case IResourceDelta.REMOVED:
					fNeedFullBuild = true;
					
					// invalidate zamia build path when it is deleted
					String bpLp = bp.getSourceFile().getLocalPath();
					String f1 = ZamiaPlugin.computeLocalPath(file);
					if (isBp = f1.equals(bpLp)) {
						sf = fakeBp;
					}
					break;
				default:
	
					isBp = name.equals("BuildPath.txt");
					 
					if (!(isBp || ZamiaProjectBuilder.fileNameAcceptable(name)))
						return false;
	
					fChangedSFs.add(sf);
					if (aDelta.getKind() == IResourceDelta.ADDED) {
						fNeedFullBuild = true;
					}
				}
				
				if (isBp) {
					fBPChanged = true;
					assert sf != null;

					// Here was an attempt to update the BP location only when it is changed
					// But, it can be changed whether as project local or link target. So
					// making the check is error-prone and makes code difficult
					//if (!sf.getLocalPath().equals(bp.getSourceFile().getLocalPath())) {
						bp.setSrc(sf);
						getProject().setPersistentProperty(BUILDPATH_QN, sf.getLocalPath());
						
						//Here we need to call on old and new build path resources to update "active buildpath" decorators.
						Display.getDefault().asyncExec(new Runnable() {
						       public void run() {
									IDecoratorManager idm = PlatformUI.getWorkbench().getDecoratorManager();
									idm.update("org.zamia.plugin.ui.BuildPathDecorator"); 
						       }
						});
						//ZamiaNavigator.refresh(500); // updates only navigator but not project explorer
						
					//}
				}
			}

			// return true to continue visiting children.
			return true;
		}

	}
	
	/**The full build is always called after clean. It also does the cleans up. However, it
	 * is often fails at ERManager.removeErrors, leaving project in bad state. We are stuck then. 
	 * We could make the cleanup safer by catching the exceptions in the ERManager. But, lets do the 
	 * clean up first, right here, where it is supposed to be. */
	protected void clean(IProgressMonitor monitor) throws CoreException {
		try {
			ZamiaProjectMap.getZamiaProject(getProject()).clean();
		} catch (Exception e) {
			throw new CoreException(new Status(Status.ERROR, ZamiaPlugin.PLUGIN_ID, "Failed to clean up the project. ", e));
		}
	}
	
	@SuppressWarnings("unchecked")
	protected IProject[] build(int aKind, Map aArgs, IProgressMonitor aMonitor) throws CoreException {

		Display d = Display.getDefault();

		logger.info("ZamiaBuilder: build() called, kind=%d", aKind);

		if (!fAutoBuildEnabled) {
			logger.info("ZamiaBuilder: auto build is currently disabled, ignoring this request.");
			return null;
		}

		long time = System.currentTimeMillis();
		if (fDisabledUntil > time) {
			logger.info("ZamiaBuilder: auto build is temporarily disabled, ignoring this request.");
			return null;
		}

		IProgressMonitor monitor = aMonitor;

		IProject project = getProject();
		ZamiaProject zprj = ZamiaProjectMap.getZamiaProject(project);
		ZamiaProjectBuilder builder = zprj.getBuilder();
		builder.setMonitor(new ZamiaMonitor(monitor));

		// BP parsing      100
		// IDX            1000
		// Local sources  1000
		// IG             1000
		// Markers         100
		// ZDB             100

		if (monitor != null) {
			monitor.beginTask("Zamia build.", 330);
		}

		boolean doFullBuild = aKind == FULL_BUILD;
		fChangedSFs = null;
		fBPChanged = false;

		if (!doFullBuild) {

			logger.info("ZamiaBuilder: Full build was not explicitly requested, will investigate...");

			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				doFullBuild = true;
				logger.info("ZamiaBuilder: No delta was given => will do full build.");
			} else {

				fNeedFullBuild = false;
				fChangedSFs = new HashSetArray<SourceFile>();

				delta.accept(new DeltaBuildDetector());

				if (fNeedFullBuild) {
					logger.info("ZamiaBuilder: Delta build detector has detected significant changes.");
					doFullBuild = true;
				} else {
					logger.info("ZamiaBuilder: Delta build detector has not detected any signifcant changes.");
				}
			}
		} else {
			logger.info("ZamiaBuilder: Full build was explicitly requested.");
		}

		int numChangedSFs = fChangedSFs != null ? fChangedSFs.size() : 0;

		logger.info("ZamiaBuilder: Situation analysis: doFullBuild=%b, bpChanged=%b, numChangedSFs=%d", doFullBuild, fBPChanged, numChangedSFs);

		for (int i = 0; i < numChangedSFs; i++) {
			logger.info("ZamiaBuilder: Situation analysis: changed source file #%2d/%d: %s", i + 1, numChangedSFs, fChangedSFs.get(i));
			if (i > 10) {
				break;
			}
		}

		if (doFullBuild || fBPChanged || numChangedSFs > 10) {

			int answer = ZamiaPlugin.askQuestion(null, "Do Full Build?", "A full project build was requested.\n\n"
					+ zprj +"\n\n"
					+ "This process can take a long time but will ensure consistency between your sources and zamiaCAD's model.\n\n"
					+ "Alternatively, you can trigger the full build by manually cleaning the project (project menu item 'Clean...') later.\n\n"
					+ "Do you want to do a full build now?", SWT.ICON_QUESTION | SWT.YES | SWT.NO);

			if (answer == SWT.NO) {
				logger.info("ZamiaBuilder: full build denied by user.");
				return null;
			} else {
				d.asyncExec(new Runnable() {
					public void run() {
						ZamiaPlugin.showConsole();
					}
				});

				ZamiaNavigator.refresh(5000);
			}
		}

		ERManager erm = zprj.getERM();

		erm.setQuiet(true);

		try {
			builder.build(doFullBuild, fBPChanged, fChangedSFs);
		} catch (Throwable e) {
			el.logException(e);
		}

		erm.setQuiet(false);

		if (doFullBuild || fBPChanged) {
			linkExternalSources(project, zprj, aMonitor);
		}

		updateMarkers(project, zprj);

		ZamiaNavigator.refresh(500);

		ZamiaEditor.updateOutlineView();
		
		//		d.asyncExec(new Runnable() {
		//			public void run() {
		//				IWorkbenchPage page = ZamiaPlugin.getWorkbenchWindow().getActivePage();
		//
		//				ZamiaNavigator zamiaNavigator = (ZamiaNavigator) page.findView(ZamiaNavigator.VIEW_ID);
		//				zamiaNavigator.refresh();
		//			}
		//		});

		if (aMonitor != null) {
			aMonitor.done();
			builder.setMonitor(null);
		}

		logger.info("ZamiaBuilder: all done.");

		return null;
	}

	private void updateMarkers(IProject aProject, ZamiaProject aZPrj) {
		logger.info("ZamiaBuilder: updating file markers");
		ZamiaErrorObserver.updateAllMarkers(aZPrj);
	}

	public static IFolder linkExternalSource(IProject aProject, String aPath, boolean aReadonly) {

		IProgressMonitor monitor = new NullProgressMonitor();

		IFolder extSrcFolder = getExtSourcesFolder(aProject, monitor, false);

		if (extSrcFolder == null || !extSrcFolder.exists()) {
			logger.error("ZamiaBuilder: failed to create %s -> bailing out.", ZamiaPlugin.BP_EXTERNAL_SOURCES);
			return null;
		}

		return linkExternalSource(aPath, aReadonly, extSrcFolder, false);
	}

	private static IFolder linkExternalSource(String aPath, boolean aReadonly, IFolder aExtSrcFolder, boolean aDoRefresh) {

		try {
			String name = aPath.replace('/', '.').substring(1);
			String host = aReadonly ? ZamiaFileSystem.ZAMIA_EFS_HOST_READONLY : ZamiaFileSystem.ZAMIA_EFS_HOST_READWRITE;

			if (Native.isWindows()) {
				name = aPath.replace("\\", ".").substring(3);
				aPath = "/" + aPath;
			}

			if (name.endsWith(".")) {
				name = name.substring(0, name.length() - 1);
			}

			URI uri = new URI(ZamiaFileSystem.ZAMIA_EFS_SCHEME, null, host, aPath.length(), aPath, null, null);

			IFolder entryFolder = aExtSrcFolder.getFolder(name);

			boolean ok = false;
			;
			try {

				URI folderURI = entryFolder.getLocationURI();

				ok = entryFolder.exists() && uri.equals(folderURI);
			} catch (Throwable t) {
			}

			if (!ok) {

				logger.debug("ZamiaBuilder: creating new folder");

				try {
					entryFolder.delete(true, null);
				} catch (Throwable e) {
				}
				entryFolder.createLink(uri, IResource.NONE, null);
			} else {
				if (aDoRefresh) {
					logger.debug("ZamiaBuilder: refreshing existing folder");
					entryFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
				}
			}

			return entryFolder;
		} catch (Throwable t) {
			el.logException(t);
		}

		return null;
	}

	private static IFolder getExtSourcesFolder(IProject aProject, IProgressMonitor aMonitor, boolean aRefresh) {
		IFolder extSrcFolder = aProject.getFolder(ZamiaPlugin.BP_EXTERNAL_SOURCES);

		if (extSrcFolder != null && aRefresh) {
			try {
				logger.debug("ZamiaBuilder: deleting %s", ZamiaPlugin.BP_EXTERNAL_SOURCES);
				extSrcFolder.delete(true, null);
			} catch (Throwable t) {
			}
		}

		if (extSrcFolder == null || aRefresh) {
			try {
				extSrcFolder = aProject.getFolder(ZamiaPlugin.BP_EXTERNAL_SOURCES);
				extSrcFolder.create(true, true, aMonitor);
			} catch (Throwable t) {
			}
		}

		//			if (extSrcFolder == null || !extSrcFolder.exists() || extSrcFolder.isLinked()) {
		//				logger.debug("ZamiaBuilder: %s is NOT ok => (re-)creating it", ZamiaPlugin.BP_EXTERNAL_SOURCES);
		//				if (extSrcFolder != null) {
		//					try {
		//						logger.debug("ZamiaBuilder: deleting %s", ZamiaPlugin.BP_EXTERNAL_SOURCES);
		//						extSrcFolder.delete(true, null);
		//					} catch (Throwable t) {
		//					}
		//				}
		//				logger.debug("ZamiaBuilder: creating %s", ZamiaPlugin.BP_EXTERNAL_SOURCES);
		//				try {
		//					extSrcFolder = aProject.getFolder(ZamiaPlugin.BP_EXTERNAL_SOURCES);
		//					extSrcFolder.create(true, true, aMonitor);
		//				} catch (Throwable t) {
		//				}
		//			}

		return extSrcFolder;
	}

	public static void linkExternalSources(IProject aProject, ZamiaProject aZPrj, IProgressMonitor aMonitor) {
		try {
			logger.info("ZamiaBuilder: re-linking %s", ZamiaPlugin.BP_EXTERNAL_SOURCES);

			// first re-create ext sources folder so we get rid of old links

			// first check if the [BP External Sources] folder itself is ok
			// if not, (re-)create it

			logger.debug("ZamiaBuilder: (re-)creating %s", ZamiaPlugin.BP_EXTERNAL_SOURCES);

			IFolder extSrcFolder = getExtSourcesFolder(aProject, aMonitor, true);

			if (extSrcFolder == null || !extSrcFolder.exists()) {
				logger.error("ZamiaBuilder: failed to create %s -> bailing out.", ZamiaPlugin.BP_EXTERNAL_SOURCES);
				return;
			}

			// now we can go about creating symlinks for each BuildPath.txt entry

			HashSet<String> generatedLinks = new HashSet<String>();

			BuildPath bp = aZPrj.getBuildPath();
			if (bp == null) {
				logger.warn("ZamiaBuilder: %s doesn't have a build path.", aZPrj);
				return;
			}

			int n = bp.getNumEntries();
			for (int i = 0; i < n; i++) {
				BuildPathEntry entry = bp.getEntry(i);

				if (aMonitor != null && aMonitor.isCanceled()) {
					logger.info("Canceled.");
					break;
				}

				if (!entry.fExtern)
					continue;

				String path;

				if (entry.fIsDirectory) {
					path = entry.fPrefix;
				} else {
					File file = new File(entry.fPrefix);
					path = file.getParent();
				}
				if (!generatedLinks.contains(path)) {
					logger.info("ZamiaBuilder: link %4d/%4d in %s points to '%s'", i + 1, n, ZamiaPlugin.BP_EXTERNAL_SOURCES, path);

					linkExternalSource(path, entry.fReadonly, extSrcFolder, true);
					generatedLinks.add(path);
				}

			}

			n = bp.getNumIncludes();
			for (int i = 0; i < n; i++) {
				String include = bp.getInclude(i);

				if (aMonitor.isCanceled()) {
					logger.info("Canceled.");
					break;
				}

				File includeFile = new File(include);

				if (!includeFile.isAbsolute())
					continue;

				File includeDirFile = includeFile.getParentFile();
				if (includeDirFile == null)
					continue;

				String path = includeDirFile.getAbsolutePath();

				if (!generatedLinks.contains(path)) {
					logger.info("ZamiaBuilder: creating include file link %4d/%4d in %s to '%s'", i + 1, n, ZamiaPlugin.BP_EXTERNAL_SOURCES, path);
					linkExternalSource(path, false, extSrcFolder, true);
					generatedLinks.add(path);
				}
			}

		} catch (Throwable t) {
			el.logException(t);
		}
	}

	public static void setAutoBuildEnabled(boolean aEnabled) {
		fAutoBuildEnabled = aEnabled;
	}

	public static void disableAutoBuild(long aMillis) {
		fDisabledUntil = System.currentTimeMillis() + aMillis;
	}

}
