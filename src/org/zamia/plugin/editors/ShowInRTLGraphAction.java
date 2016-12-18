/*
 * Copyright 2007-2009 by the authors indicated in the @author tags.
 * All rights reserved.
 *
 * See the LICENSE file for details.
 *
 */

package org.zamia.plugin.editors;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.zamia.ASTNode;
import org.zamia.BuildPath;
import org.zamia.DMManager;
import org.zamia.ExceptionLogger;
import org.zamia.SourceLocation;
import org.zamia.Toplevel;
import org.zamia.ZamiaException;
import org.zamia.ZamiaLogger;
import org.zamia.analysis.SourceLocation2AST;
import org.zamia.plugin.ZamiaPlugin;
import org.zamia.plugin.views.rtl.RTLView;
import org.zamia.rtl.RTLManager;
import org.zamia.rtl.RTLModule;
import org.zamia.rtl.RTLNode;
import org.zamia.rtl.RTLSignal;
import org.zamia.vhdl.ast.Architecture;
import org.zamia.vhdl.ast.Entity;
import org.zamia.vhdl.ast.Name;
import org.zamia.vhdl.ast.SignalDeclaration;

/**
 * 
 * @author Guenter Bartsch
 * 
 */

public class ShowInRTLGraphAction extends StaticAnalysisAction {

	public static final ZamiaLogger logger = ZamiaLogger.getInstance();

	public static final ExceptionLogger el = ExceptionLogger.getInstance();

	static ZamiaEditor fEditor;

	static Shell fShell;

	static Display fDisplay;

	class ShowInRTLGraphJob extends Job {

		private ShowInRTLGraphAction fAction;

		public ShowInRTLGraphJob(ShowInRTLGraphAction aAction) {
			super("Show in RTL Graph");
			fAction = aAction;
		}

		protected IStatus run(IProgressMonitor monitor) {
			try {
				ASTNode nearest = SourceLocation2AST.findNearestASTNode(fAction.getLocation(), true, fZPrj);
				if (nearest != null) {

					fAction.showInRTLGraph(nearest);

				} else {

					ZamiaPlugin.showError(fShell, "No Intermediate Object", "Current source location does not correspond to any intermediate object.", "Comment/Whitespace?");

				}
			} catch (IOException e) {
				el.logException(e);
			} catch (ZamiaException e) {
				ZamiaPlugin.showError(fShell, "Zamia Exception Caught", e.getMessage(), "");
				el.logException(e);
			}

			return Status.OK_STATUS;
		}
	}

	private RTLView fView;

	private RTLModule fRTLG;

	ASTNode fIO;

	//	private SimulatorView fSimView;

	public void setActiveEditor(IAction aAction, IEditorPart aTargetEditor) {
		if (aTargetEditor == null) {
			return;
		}
		if (!(aTargetEditor instanceof ZamiaEditor)) {
			return;
		}
		fEditor = (ZamiaEditor) aTargetEditor;
		fShell = fEditor.getSite().getShell();
		fDisplay = fShell.getDisplay();
	}

	public void selectionChanged(IAction aAction, ISelection aSelection) {
		aAction.setEnabled(true);
	}

	public void run(IAction aAction) {

		try {
			processSelection();

			fView = ZamiaPlugin.showRTLView();

			if (fView != null) {

				ShowInRTLGraphJob job = new ShowInRTLGraphJob(this);
				job.setPriority(Job.SHORT);
				job.schedule();
			}

		} catch (Throwable e) {
			el.logException(e);
		}
	}

	private void showInRTLGraph(ASTNode aIO) {

		//throw new RuntimeException("Sorry, RTL code is disabled for now.");

		fIO = aIO;

		DMManager dum = fZPrj.getDUM();

		// search architecture

		ASTNode io = aIO;
		Architecture arch = null;
		while (io != null) {
			if (io instanceof Architecture) {
				arch = (Architecture) io;
				break;
			}
			if (io instanceof Entity) {
				Entity entity = (Entity) io;

				String entityId = entity.getId();
				String libId = entity.getLibId();

				try {
					arch = dum.getArchitecture(libId, entityId);
				} catch (ZamiaException e) {
					el.logException(e);
					return;
				}

				break;
			}

			io = io.getParent();
		}

		if (arch == null) {
			logger.error("showInRTLGraph(): arch not found :(");
			return;
		}

		if (fView != null) {

			fRTLG = fView.getRTLModule();

			//			if (fRTLG == null || fRTLG.getArch() != arch) {
			// FIXME: check if source has changed
			if (true) {
				
				


				// wrong rtl graph => elaborate the right one

				//try {
					//fRTLG = fZPrj.elaborate(arch.getLibId(), arch.getEntityName().getId(), arch.getId());
				 RTLManager rtlman = fZPrj.getRTLM();
				 BuildPath bp = fZPrj.getBuildPath();
				 
				 if(bp.getNumToplevels() == 0)
				 {
					 logger.info("Num of toplevel == 0");
					 return;
				 }
				 Toplevel aTL = bp.getToplevel(0);
				 try {
					 
					 rtlman.clean();
					 fRTLG = rtlman.buildRTL(aTL, null, 1000);
					 
					
					
				} catch (ZamiaException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				 
					
					fDisplay.asyncExec(new Runnable() {
						public void run() {

							fView.setRTLModule(fRTLG);

//							if (fSimView != null) {
//								fSimView.setRTLGraph(fRTLG);
//							}

							SourceLocation l1 = fIO.getLocation();
							if (l1 == null) {
								ZamiaPlugin.showError(fView.getSite().getShell(), "No Source Information", "No source information found.",
										"Internal Error: Intermediate object does not have source information.");
								return;
							}

							boolean found = false;

							if (fRTLG != null) {
								if (fIO instanceof Name) {
									String id = ((Name) fIO).getId();

									RTLSignal signal = fRTLG.findSignal(id);

									// RTLSignal signal = searchSignal(rtlg, l1,
									// id);

									if (signal != null) {
										fView.addHighlight(signal);
										found = true;
									}
								}

								if (!found) {
									if (fIO instanceof SignalDeclaration) {

										SignalDeclaration sd = (SignalDeclaration) fIO;

										String id = sd.getId();
										RTLSignal signal = fRTLG.findSignal(id);

										// RTLSignal signal = searchSignal(rtlg, l1,
										// id);

										if (signal != null) {
											fView.addHighlight(signal);
											found = true;
										}

									}
								}

								if (!found) {
									RTLModule module = searchModule(fRTLG, l1);

									if (module != null) {

										fView.addHighlight(module);
										found = true;
									}
								}
							}

							if (!found) {
								logger.error("No module was found that originated from this source location.");
							}
						}
					});
//				} catch (IOException e) {
//					el.logException(e);
//					return;
//				} catch (ClassNotFoundException e) {
//					el.logException(e);
//					return;
//				} catch (ZamiaException e) {
//					el.logException(e);
//					ZamiaPlugin.showError(fShell, "Error elaborating architecture", e.getMessage(), "");
//					return;
//				}

			}

		}

	}

 private RTLSignal searchSignal(RTLModule rtlg_, SourceLocation l1_, String id_) {

 RTLSignal signal =null	; 
 int n = rtlg_.getNumSignals();
 for (int i = 0; i < n; i++) {
 
	 signal = rtlg_.getSignal(i);

// IntermediateObject src2 = signal.getSource();
// if (src2 == null)
// continue;
// SourceLocation l2 = src2.getLocation();
//
// String p1 = l2.sf.getAbsolutePath();
// String p2 = l1_.sf.getAbsolutePath();
//
// if (p1.equals(p2)) {
//
// if (signal.getId().equals(id_))
 }
 return signal;

 }



	private RTLModule searchModule(RTLModule aRTLG, SourceLocation aLocation) {

		int n = aRTLG.getNumNodes(); //.getNumSubs();
		for (int i = 0; i < n; i++) {
			RTLNode module = aRTLG.getNode(i); //getSub(i);

//			ASTObject src2 = module.get.getSource();
//			if (src2 == null)
//				continue;
//			SourceLocation l2 = src2.getLocation();
//
//			if (haveMatch(aLocation, l2)) {
//				return module;
//			}
		}
		return null;
	}

	private boolean haveMatch(SourceLocation aLocation1, SourceLocation aLocation2) {

		if (!aLocation1.fSF.getFileName().equals(aLocation2.fSF.getFileName()))
			return false;

		return aLocation1.fLine == aLocation2.fLine && aLocation1.fCol == aLocation2.fCol;
	}

}
