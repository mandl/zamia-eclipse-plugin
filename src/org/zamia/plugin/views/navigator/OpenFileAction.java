/*
 * Copyright 2007-2009,2011 by the authors indicated in the @author tags.
 * All rights reserved.
 *
 * See the LICENSE file for details.
 */

package org.zamia.plugin.views.navigator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.IDE;
import org.zamia.ExceptionLogger;
import org.zamia.SourceLocation;
import org.zamia.Toplevel;
import org.zamia.ToplevelPath;
import org.zamia.ZamiaException;
import org.zamia.ZamiaLogger;
import org.zamia.ZamiaProject;
import org.zamia.plugin.ZamiaPlugin;
import org.zamia.plugin.ZamiaProjectMap;
import org.zamia.plugin.editors.ZamiaEditor;
import org.zamia.plugin.views.rtl.RTLView;
import org.zamia.rtl.RTLManager;
import org.zamia.rtl.RTLModule;
import org.zamia.vhdl.ast.Architecture;
import org.zamia.vhdl.ast.DMUID;
import org.zamia.vhdl.ast.Entity;

/**
 * 
 * @author Guenter Bartsch
 * 
 */
public class OpenFileAction extends Action {

	public final static ExceptionLogger el = ExceptionLogger.getInstance();

	public final static ZamiaLogger logger = ZamiaLogger.getInstance();

	private List<IFile> fFileSelected;

	private List<IGModuleWrapper> fIGWrappersSelected;

	private List<RTLModuleWrapper> fRTLWrappersSelected;
	
	private List<Architecture> fArchitecture;
	
	private List<Entity> fEntity;

	private List<Object> fContainersSelected; // IContainer or IWrappedResource

	private ISelectionProvider fProvider;

	private IWorkbenchPage fPage;

	public OpenFileAction(IWorkbenchPage aPage, ISelectionProvider aSelectionProvider) {
		this.setText("Open With Zamia Editor");
		fProvider = aSelectionProvider;
		fPage = aPage;
	}

	public boolean isEnabled() {
		return true;
	}

	public void run() {
		fillSelections();

		if (fFileSelected.size() > 0) {

			IFile file = fFileSelected.get(0);

			try {
				IDE.openEditor(fPage, file);
			} catch (Exception e) {
				el.logException(e);
			}

		} else if (fIGWrappersSelected.size() > 0) {

			IGModuleWrapper wrapper = fIGWrappersSelected.iterator().next();

			ZamiaProject zprj = wrapper.getZPrj();

			IProject prj = ZamiaProjectMap.getProject(zprj);

			SourceLocation location = wrapper.getLocation();

			if (location == null) {
				logger.error("OpenFileAction: wrapper doesn't give me a location: %s", wrapper);
				return;
			}

			IEditorPart editPart = ZamiaPlugin.showSource(fPage, prj, location, 0);

			if (editPart instanceof ZamiaEditor) {

				ZamiaEditor editor = (ZamiaEditor) editPart;

				ToplevelPath path = wrapper.getEditorPath();

				if (path != null) {
					//editor.setPath(wrapper.isBlueIG() ? path : path.getParent());
					editor.setPath(path);
				}

			}

		} else if (fRTLWrappersSelected.size() > 0) {

			RTLModuleWrapper wrapper = fRTLWrappersSelected.iterator().next();

			DMUID dmuid = wrapper.getDMUID();
			
			ZamiaProject zprj = wrapper.getZPrj();

			RTLManager manager = zprj.getRTLM();
			
			Toplevel tl = new Toplevel(dmuid, null);
			
			RTLModule rtlm = manager.findModule(tl);;
			
			RTLView rtlv = ZamiaPlugin.showRTLView();
			
			rtlv.setRTLModule(rtlm);
			
		} else if (fArchitecture.size() > 0) {
			Architecture wrapper = fArchitecture.iterator().next();
			
			ZamiaProject zprj = wrapper.getZPrj();

			IProject prj = ZamiaProjectMap.getProject(zprj);

			SourceLocation location = wrapper.getLocation();

			if (location == null) {
				logger.error("OpenFileAction: wrapper doesn't give me a location: %s", wrapper);
				return;
			}

			IEditorPart editPart = ZamiaPlugin.showSource(fPage, prj, location, 0);

			if (editPart instanceof ZamiaEditor) {

				ZamiaEditor editor = (ZamiaEditor) editPart;				

			}
			
		
	} else if (fEntity.size() > 0) {
		Entity wrapper = fEntity.iterator().next();
		
		ZamiaProject zprj = wrapper.getZPrj();

		IProject prj = ZamiaProjectMap.getProject(zprj);

		SourceLocation location = wrapper.getLocation();

		if (location == null) {
			logger.error("OpenFileAction: wrapper doesn't give me a location: %s", wrapper);
			return;
		}

		IEditorPart editPart = ZamiaPlugin.showSource(fPage, prj, location, 0);

		if (editPart instanceof ZamiaEditor) {

			ZamiaEditor editor = (ZamiaEditor) editPart;

			

		}

		
	}
		else if (fContainersSelected.size() > 0) {
			if (this.fProvider instanceof TreeViewer) {
				TreeViewer viewer = (TreeViewer) this.fProvider;
				for (Object container : fContainersSelected) {
					if (viewer.isExpandable(container)) {
						viewer.setExpandedState(container, !viewer.getExpandedState(container));
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void fillSelections() {

		fFileSelected = new ArrayList<IFile>();
		fContainersSelected = new ArrayList<Object>();
		fIGWrappersSelected = new ArrayList<IGModuleWrapper>();
		fRTLWrappersSelected = new ArrayList<RTLModuleWrapper>();
		fArchitecture = new ArrayList<Architecture>();
		fEntity = new ArrayList<Entity>();
		ISelection selection = fProvider.getSelection();
		if (!selection.isEmpty()) {
			IStructuredSelection sSelection = (IStructuredSelection) selection;

			Iterator<Object> iterator = sSelection.iterator();
			while (iterator.hasNext()) {
				Object element = iterator.next();

				if (element instanceof IGModuleWrapper) {

					IGModuleWrapper wrapper = (IGModuleWrapper) element;

					fIGWrappersSelected.add(wrapper);
				} else if (element instanceof IAdaptable) {
					IAdaptable adaptable = (IAdaptable) element;
					IFile file = (IFile) adaptable.getAdapter(IFile.class);
					if (file != null) {
						fFileSelected.add(file);

					} else {
						IContainer container = (IContainer) adaptable.getAdapter(IContainer.class);
						if (container != null) {
							fContainersSelected.add(element);
						}
					}
				} else if (element instanceof RTLModuleWrapper) {

					RTLModuleWrapper wrapper = (RTLModuleWrapper) element;

					fRTLWrappersSelected.add(wrapper);
				}
				else if(element instanceof Architecture)
				{
					Architecture wrapper = (Architecture) element;
					fArchitecture.add(wrapper);
				}
				else if(element instanceof Entity)
				{
					Entity wrapper = (Entity) element;
					fEntity.add(wrapper);;
				}
				
			}
		}
	}

}
