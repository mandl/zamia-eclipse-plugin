/*
 * Copyright 2004-2010 by the authors indicated in the @author tags.
 * All rights reserved.
 *
 * See the LICENSE file for details.
 *
 */

package org.zamia.plugin.editors;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.PaintManager;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.IVerticalRulerColumn;
import org.eclipse.jface.text.source.MatchingCharacterPainter;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.ContentOutline;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
//import org.eclipse.ui.views.contentoutline.ContentOutline;
//import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.zamia.BuildPath;
import org.zamia.DMManager;
import org.zamia.ExceptionLogger;
import org.zamia.SFDMInfo;
import org.zamia.SourceFile;
import org.zamia.SourceLocation;
import org.zamia.Toplevel;
import org.zamia.ToplevelPath;
import org.zamia.ZamiaException;
import org.zamia.ZamiaLogger;
import org.zamia.ZamiaProject;
import org.zamia.instgraph.IGInstantiation;
import org.zamia.instgraph.IGItem;
import org.zamia.instgraph.IGManager;
import org.zamia.instgraph.IGModule;
import org.zamia.instgraph.sim.IGISimulator;
import org.zamia.instgraph.sim.annotations.IGSimAnnotation;
import org.zamia.instgraph.sim.annotations.IGSimAnnotator;
import org.zamia.plugin.ZamiaPlugin;
import org.zamia.plugin.ZamiaProjectMap;
import org.zamia.plugin.editors.annotations.AnnotatedDocument;
import org.zamia.plugin.editors.buildpath.BasicViewerConfiguration.BasicIdentifierScanner;
import org.zamia.plugin.views.sim.SimulatorView;
import org.zamia.util.HashSetArray;
import org.zamia.util.PathName;
import org.zamia.vhdl.ast.DMUID;
import org.zamia.vhdl.ast.DMUID.LUType;
import org.zamia.vhdl.ast.VHDLNode;


/**
 * 
 * @author Guenter Bartsch
 * 
 */
public class ZamiaEditor extends ErrorMarkEditor implements IShowInTargetList {

	public final static ZamiaLogger logger = ZamiaLogger.getInstance();

	public final static ExceptionLogger el = ExceptionLogger.getInstance();

	private ZamiaOutlinePage fOutlinePage;

	private ProjectionSupport fProjectionSupport;

	private ProjectionAnnotationModel fAnnotationModel;

	private Annotation[] fOldAnnotations;

	private ZamiaReconcilingStrategy fReconcilingStrategy;

	protected MatchingCharacterPainter fBracketPainter;

	private PaintManager fPaintManager;

	private static final RGB BRACKETS_COLOR = new RGB(160, 160, 160);

	private final static char[] BRACKETS = { '{', '}', '(', ')', '[', ']' };

	private Text fPathEdit;

	private ToplevelPath fPath;

	private SimulatorView fSimView;

	private IGISimulator fSim;

	private Button fAnnotateCheck;

	private Button fAnnotateUpdate;

	private int fCaretPos;

	private int fLine;

	private int fCol;

	private boolean fIsAnnotating;

	private boolean fIsAnnotated = false;

	private HashSetArray<IGSimAnnotation> fAnns;

	private ZamiaProject fZPrj;

	private Button fGoUpButton;

	private SourceFile fSF;

	public ZamiaEditor(BasicIdentifierScanner aScanner, String[] fDefaultPrefixes) {
		super();
		fReconcilingStrategy = new ZamiaReconcilingStrategy(this);
		setSourceViewerConfiguration(new ZamiaSourceViewerConfiguration(aScanner, fReconcilingStrategy, fDefaultPrefixes, this));
	}

	protected void initializeEditor() {
		super.initializeEditor();
		setEditorContextMenuId("#ZamiaTextEditorContext"); //$NON-NLS-1$
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);

		String filename = getFilename();

		if (filename != null && fZPrj != null) {

			fSF = null;
			if (input instanceof ExternalReaderEditorInput) {
				ExternalReaderEditorInput esei = (ExternalReaderEditorInput) input;
				fSF = new SourceFile(esei.getURI());
			} else {
				IFile file = ResourceUtil.getFile(input);
				fSF = ZamiaPlugin.getSourceFile(file);
			}

			String path = fZPrj.lookupEditorPath(filename);

			if (path != null) {
				try {
					fPath = new ToplevelPath(path);
				} catch (ZamiaException e) {
					el.logException(e);
				}
			} else {

				try {
					/*
					 * try to use the default path of a IGModule corresponding to our DUUID
					 * 
					 */
					//IProject prj = null;
					//SourceFile sf = new SourceFile(new File(filename));

					IGManager igm = fZPrj.getIGM();
					DMManager dum = fZPrj.getDUM();

					SFDMInfo info = dum.compileFile(fSF, null);

					if (info != null) {
						// use first architecture we can find that provides a path

						for (DMUID duuid : info) {

							if (duuid.getType() != LUType.Architecture) {
								continue;
							}

							String signature = IGInstantiation.computeSignature(duuid, null);

							IGModule module = igm.findModule(signature);
							if (module != null) {
								fPath = module.getStructure().getPath();
							}

							if (fPath != null) {
								break;
							}
						}
					}
				} catch (Exception e) {
					el.logException(e);
				}

			}
		}
	}

	@Override
	public void createPartControl(Composite aParent) {

		GridLayout gl = new GridLayout();
		aParent.setLayout(gl);
		gl.numColumns = 1;
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0;

		Composite comp = new Composite(aParent, SWT.NONE);

		gl = new GridLayout();
		gl.numColumns = 5;
		gl.horizontalSpacing = 2;
		gl.verticalSpacing = 0;
		gl.marginHeight = 2;
		gl.marginWidth = 2;
		comp.setLayout(gl);
		// comp.setBackground(aParent.getShell().getDisplay().getSystemColor(SWT.COLOR_BLUE));

		setFillLayout(comp, false);

		createLabel("Path:", comp);
		fPathEdit = new Text(comp, SWT.BORDER);
		setFillLayout(fPathEdit, false);
		fPathEdit.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.KEYPAD_CR || e.keyCode == SWT.CR) {
					navigateTo(fPathEdit.getText().toUpperCase());
					fPathEdit.setText(fPath != null ? fPath.toString() : "");
					getTextWidget().forceFocus();
				}
			}
		});
		
		if (fPath != null) {
			fPathEdit.setText(fPath.toString());
		}

		fGoUpButton = new Button(comp, SWT.PUSH);
		setLeftLayout(fGoUpButton);
		Image icon = ZamiaPlugin.getImage("/share/images/up.gif");
		fGoUpButton.setImage(icon);
		fGoUpButton.setEnabled(false);
		fGoUpButton.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent aE) {
			}

			public void widgetSelected(SelectionEvent aE) {

				if (fPath == null)
					return;

				ToplevelPath tlp = fPath.getParent();

				fZPrj = fReconcilingStrategy.getZPrj();

				IGManager igm = fZPrj.getIGM();

				IGItem item = igm.findItem(tlp);

				if (item != null) {

					SourceLocation location = item.computeSourceLocation();

					IWorkbenchPage page = getSite().getPage();

					IProject prj = ZamiaProjectMap.getProject(fZPrj);

					IEditorPart ep = ZamiaPlugin.showSource(page, prj, location, 0);

					if (ep instanceof ZamiaEditor) {
						ZamiaEditor editor = (ZamiaEditor) ep;

						editor.setPath(tlp);
					}
				}
			}
		});

		fAnnotateCheck = new Button(comp, SWT.CHECK);
		setLeftLayout(fAnnotateCheck);
		fAnnotateCheck.setText("Annotate");
		fAnnotateCheck.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent aE) {
			}

			public void widgetSelected(SelectionEvent aE) {
				if (fAnnotateCheck.getSelection()) {
					try {
						annotate();
					} catch (ZamiaException e) {
						el.logException(e);
					}
				} else {
					removeAnnotation();
				}
			}

		});

		fAnnotateUpdate = new Button(comp, SWT.PUSH);
		setLeftLayout(fAnnotateUpdate);
		fAnnotateUpdate.setText("Update");
		fAnnotateUpdate.setEnabled(false);
		fAnnotateUpdate.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent aE) {
			}

			public void widgetSelected(SelectionEvent aE) {
				updateAnnotation();
			}
		});
		
		comp.pack();

		Composite editorContainer = new Composite(aParent, SWT.NONE);
		setFillLayout(editorContainer, true);
		FillLayout fl = new FillLayout();
		editorContainer.setLayout(fl);
		super.createPartControl(editorContainer);

		ProjectionViewer viewer = (ProjectionViewer) getSourceViewer();
		fProjectionSupport = new ProjectionSupport(viewer, getAnnotationAccess(), getSharedColors());
		fProjectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.error");
		fProjectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.warning");
		fProjectionSupport.install();
		viewer.doOperation(ProjectionViewer.TOGGLE);

		fAnnotationModel = viewer.getProjectionAnnotationModel();

		fPaintManager = new PaintManager(getSourceViewer());

		ISourceViewer sourceViewer = getSourceViewer();

		fBracketPainter = new MatchingCharacterPainter(sourceViewer, new ZamiaPairMatcher(BRACKETS));
		fBracketPainter.setColor(ColorManager.getInstance().getColor(BRACKETS_COLOR));
		fPaintManager.addPainter(fBracketPainter);

		DebugReportVisualizer visualizer = DebugReportVisualizer.getInstance(fReconcilingStrategy.getZPrj());

		visualizer.highlightDeprecatedLines(this);

		visualizer.highlightAssignments(this);

		//		StyledText styledText = viewer.getTextWidget();
		//		styledText.setLineSpacing(2);
	}

	/**Overriden to disable editing of annotated text.*/
	public boolean isEditable() {
		return !(fIsAnnotated || fIsAnnotating) && super.isEditable();
	}
	
	private void setFillLayout(Control comp, boolean fillVertical) {
		comp.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, fillVertical));
	}

	private void setLeftLayout(Control control) {
		GridData gd = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
		control.setLayoutData(gd);
	}
	
	//This method is also used in Philip branch
	private void createLabel(String name, Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(name);
		setLeftLayout(label);
	}

	void addHitCountColumn(IVerticalRulerColumn aHitCountColumn) {

		IVerticalRuler verticalRuler = getVerticalRuler();
		if (verticalRuler == null) {
			verticalRuler = super.createVerticalRuler();
		}

		if (verticalRuler instanceof CompositeRuler) {
			CompositeRuler compositeRuler = (CompositeRuler) verticalRuler;

			TreeSet<Integer> toBeRemoved = new TreeSet<Integer>();
			Iterator<?> decoratorIterator = compositeRuler.getDecoratorIterator();
			int count = 0;
			while (decoratorIterator.hasNext()) {
				Object decorator = decoratorIterator.next();
				if (decorator instanceof NumberedVerticalRulerColumn) {
					toBeRemoved.add(count);
				}
				count++;
			}
			count -= toBeRemoved.size();
			Iterator<Integer> iterator = toBeRemoved.descendingIterator();
			while (iterator.hasNext()) {
				int i = iterator.next();
				compositeRuler.removeDecorator(i);
			}

			if (aHitCountColumn != null) {
				compositeRuler.addDecorator(count, aHitCountColumn);
			}
		}

		verticalRuler.update();
	}

	StyledText getTextWidget() {
		return getSourceViewer().getTextWidget();
	}

	protected void createActions() {
		super.createActions();

		IAction a = new TextOperationAction(ZamiaPlugin.getDefault().getResourceBundle(), "ContentAssistProposal.", this, ISourceViewer.CONTENTASSIST_PROPOSALS); //$NON-NLS-1$
		a.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction("ContentAssistProposal", a); //$NON-NLS-1$

		a = new TextOperationAction(ZamiaPlugin.getDefault().getResourceBundle(), "ContentAssistTip.", this, ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION); //$NON-NLS-1$
		a.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
		setAction("ContentAssistTip", a); //$NON-NLS-1$
	}

	protected ISourceViewer createSourceViewer(Composite aParent, IVerticalRuler aRuler, int aStyles) {
		ISourceViewer viewer = new ProjectionViewer(aParent, aRuler, getOverviewRuler(), isOverviewRulerVisible(), aStyles);
		// ensure decoration support has been created and configured.
		getSourceViewerDecorationSupport(viewer);
		return viewer;
	}

	public void updateFoldingStructure(ArrayList<Position> aPositions) {
		Annotation[] annotations = new Annotation[aPositions.size()];

		// this will hold the new annotations along
		// with their corresponding positions
		HashMap<ProjectionAnnotation, Position> newAnnotations = new HashMap<ProjectionAnnotation, Position>();

		for (int i = 0; i < aPositions.size(); i++) {
			ProjectionAnnotation annotation = new ProjectionAnnotation();

			newAnnotations.put(annotation, aPositions.get(i));

			annotations[i] = annotation;
		}

		if (fAnnotationModel != null)
			fAnnotationModel.modifyAnnotations(fOldAnnotations, newAnnotations, null);

		fOldAnnotations = annotations;
	}

	/**
	 * Get the content outline page if requested.
	 * 
	 */
	public Object getAdapter(@SuppressWarnings("rawtypes") Class aClass) {
		Object adapter;
		if (aClass.equals(IContentOutlinePage.class)) {
			if ((fOutlinePage == null) || fOutlinePage.isDisposed()) {
				fOutlinePage = new ZamiaOutlinePage(this);
				
				new AbstractSelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						IWorkbenchPart part= getSite().getWorkbenchWindow().getPartService().getActivePart();
						boolean outlineIsActive = part instanceof ContentOutline && ((ContentOutline)part).getCurrentPage() == fOutlinePage;
						if (!outlineIsActive) {
							computeCursorPos(getDocument(), (TextSelection) event.getSelection());
							fOutlinePage.select(new SourceLocation(fSF, fLine + 1, fCol+1));
						}
					}
				}.install(getSelectionProvider());
				
				if (getEditorInput() != null) {
					fOutlinePage.setInput(getEditorInput());
				}
			}

			adapter = fOutlinePage;
		} else {
			adapter = super.getAdapter(aClass);
		}

		return adapter;
	}

	public void updateOutlinePage() {
		if (fOutlinePage != null) {
			fOutlinePage.update();
		}
	}

	/**
	 * The <code>VHDLEditor</code> implementation of this method performs any
	 * extra disposal actions required by the VHDL editor.
	 * 
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 * 
	 */
	public void dispose() {
		if (fOutlinePage != null && !fOutlinePage.isDisposed()) {
			fOutlinePage.dispose();
			fOutlinePage = null;
		}
		super.dispose();
	}

	public IDocument getDocument() {
		IDocument doc = this.getSourceViewer().getDocument();
		return doc;
	}

	public void selectionChanged(IWorkbenchPart aPart, ISelection aSelection) {

	}

	public ZamiaReconcilingStrategy getReconcilingStrategy() {
		return fReconcilingStrategy;
	}

	public boolean isShowingCoverage() {
		return findSimulatorView().doShowCoverage();
	}

	public void outlineSelectionChanged(ISelection selection) {
		
		Object selectedObject = ((IStructuredSelection) selection).getFirstElement();

		if (selectedObject instanceof VHDLNode) {
			VHDLNode io = (VHDLNode) selectedObject;

			SourceLocation location = io.getLocation();
			if (location != null) {

				try {
					
					int line = location.fLine;
					if (isAnnotated()) {
						line *= 2;
					}
					line--;
					
					int offset = getDocument().getLineOffset(line) + location.fCol - 1;
					selectAndReveal(offset, 1);
					
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}

	@Override
	protected void initializeKeyBindingScopes() {
		setKeyBindingScopes(new String[] { "org.zamia.plugin.ZamiaEditorScope" });
	}

	public String[] getShowInTargetIds() {
		//return new String[] { ProjectExplorer.VIEW_ID, IPageLayout.ID_RES_NAV, "org.zamia.plugin.views.navigator.ZamiaNavigator" };
		return new String[] { ProjectExplorer.VIEW_ID, "org.zamia.plugin.views.navigator.ZamiaNavigator" };
	}

	public void updateColors() {
		// FIXME: implement
	}

	public void setPath(ToplevelPath aPath) {
		if (aPath == null) {
			//			fPathEdit.setText(" *** NO PATH ***");
			//			fPath = null;
			//			fAnnotateCheck.setEnabled(false);
			//			fGoUpButton.setEnabled(false);
			return;
		}

		fPath = aPath;
		fPathEdit.setText(aPath.toString());
		fAnnotateCheck.setEnabled(true);
		fGoUpButton.setEnabled(true);

		String filename = getFilename();

		if (filename != null) {
			fZPrj.storeEditorPath(filename, aPath.toString());
		}
	}

	public String getFilename() {
		String filename = null;
		IEditorInput input = getEditorInput();

		IProject prj = null;

		if (input instanceof FileEditorInput) {

			FileEditorInput fei = (FileEditorInput) input;
			IFile file = fei.getFile();
			if (file != null) {

				IPath rawLocation = file.getRawLocation();

				//				IPath rp = file.getProjectRelativePath();
				//
				//				if (rp != null) {
				//					ZamiaPlugin.showSource(
				//				}
				//
				filename = rawLocation != null ? rawLocation.toOSString() : file.toString();

				prj = file.getProject();
			}
		} else if (input instanceof ExternalReaderEditorInput) {
			ExternalReaderEditorInput erei = (ExternalReaderEditorInput) input;
			filename = erei.getName();
			prj = erei.getProject();
		}

		if (prj != null) {
			fZPrj = ZamiaProjectMap.getZamiaProject(prj);
		}

		return filename;
	}

	public ZamiaProject getZPrj() {
		return fZPrj;
	}

	public SourceFile getSourceFile() {
		return fSF;
	}

	public SimulatorView findSimulatorView() {

		if (fSimView == null) {

			fSimView = ZamiaPlugin.findView("org.zamia.plugin.views.sim.SimulatorView");
			if (fSimView == null)
				try {
					fSimView  = (SimulatorView) ZamiaPlugin.getPage().showView("org.zamia.plugin.views.sim.SimulatorView");
				} catch (PartInitException e) {
					ExceptionLogger.getInstance().logException(e);
				}
		}

		return fSimView;
	}

	public IGISimulator findSimulator() {
		SimulatorView view = findSimulatorView();

		if (view == null) {
			return null;
		}

		fSim = view.getSim();

		return fSim;
	}

	public void annotate() throws ZamiaException {

		logger.info("ZamiaEditor: annotate() start...");

		if (fSF == null) {
			logger.error("ZamiaEditor: Do not have a source file (fSF==null).");
			return;
		}

		if (fIsAnnotating) {
			logger.error("ZamiaEditor: I am currently busy annotating, ignoring new annotate job.");
			return;
		}

		fIsAnnotating = true;

		final IDocument originalDocument = getDocument();

		/*
		 *  remember cursor position
		 */

		computeCursorPos(originalDocument);

		final ISourceViewer viewer = getSourceViewer();

		fLine *= 2;
		fLine++;

		SourceViewerConfiguration cfg = getSourceViewerConfiguration();
		final int tabWidth = cfg.getTabWidth(viewer);

		fZPrj = fReconcilingStrategy.getZPrj();

		IGISimulator sim = findSimulator();
		if (sim == null) {
			logger.error("ZamiaEditor: No simulator active.");
			fIsAnnotating = false;
			return;
		}

		Job job = new Job("Annotate source code") {

			@Override
			protected IStatus run(IProgressMonitor aMonitor) {

				IGSimAnnotator annotator = new IGSimAnnotator(fZPrj);

				BigInteger cursorTime = fSimView.getCursorTime();

				if (!annotator.genAnnotationsEnv(fSF, getPath(), fSim, cursorTime)) {
					logger.error("ZamiaEditor: annotation: genAnnotationsEnv() failed.");
					fIsAnnotating = false;
					return Status.OK_STATUS;
				}

				if (aMonitor != null && aMonitor.isCanceled()) {
					fIsAnnotating = false;
					return Status.OK_STATUS;
				}

				fAnns = annotator.genAnnotations();

				logger.info("ZamiaEditor: generating annotated document...");

				Display.getDefault().syncExec(new Runnable() {

					public void run() {
						AnnotatedDocument annotatedDoc = new AnnotatedDocument(originalDocument.get(), fAnns, tabWidth, fSimView);

						viewer.setDocument(annotatedDoc);

						fAnnotateUpdate.setEnabled(true);

						setCursor(annotatedDoc, ZamiaEditor.this);

						fAnnotateCheck.setEnabled(true);
						fAnnotateCheck.setSelection(true);
					}
				});

				logger.info("ZamiaEditor: annotate(): done.");

				fIsAnnotating = false;
				fIsAnnotated = true;
				return Status.OK_STATUS;
			}

		};

		job.setPriority(Job.SHORT);
		job.schedule();
		
		//refresh editable state
		updateState(getEditorInput());
		
	}

	private void computeCursorPos(IDocument doc, ITextSelection selection) {
		fCaretPos = selection.getOffset();
		fLine = 0;
		try {
			fLine = doc.getLineOfOffset(fCaretPos);
		} catch (BadLocationException e1) {
			el.logException(e1);
		}
		fCol = 0;
		try {
			fCol = fCaretPos - doc.getLineOffset(fLine);
			if (fCol < 0) {
				fCol = 0;
			}
		} catch (BadLocationException e1) {
			el.logException(e1);
		}
	}
	
	private void computeCursorPos(IDocument originalDocument) {
		computeCursorPos(originalDocument, (ITextSelection) getSelectionProvider().getSelection());
	}

	public boolean isAnnotated() {
		return fIsAnnotated;
	}

	private void updateAnnotation() {
		ToplevelPath path = getPath();

		if (path != null) {

			IDocument doc = getDocument();

			computeCursorPos(doc);
			fLine = fLine / 2;

			getSite().getPage().closeEditor(this, false);
			ZamiaEditor editor = (ZamiaEditor) ZamiaPlugin.showSource(getSite().getPage(), fZPrj, path);

			setCursor(editor.getDocument(), editor);

			try {
				editor.annotate();
			} catch (ZamiaException e) {
				el.logZamiaException(e);
			}
		}
	}

	private void removeAnnotation() {

		ToplevelPath path = getPath();

		if (path != null) {

			IDocument doc = getDocument();

			computeCursorPos(doc);

			fLine = fLine / 2;

			getSite().getPage().closeEditor(this, false);
			ZamiaEditor editor = (ZamiaEditor) ZamiaPlugin.showSource(getSite().getPage(), fZPrj, path);

			setCursor(editor.getDocument(), editor);
		}
		
	}

	private void setCursor(IDocument document, TextEditor editor) {
		try {
			fCaretPos = document.getLineOffset(fLine) + fCol;
		} catch (BadLocationException e) {
			el.logException(e);
		}

		editor.selectAndReveal(fCaretPos, 0);
	}

	public ToplevelPath getPath() {
		return fPath;
	}

	private void navigateTo(String aPathStr) {

		ToplevelPath tlp = null;

		if (aPathStr.contains(":")) {

			try {
				tlp = new ToplevelPath(aPathStr);

			} catch (ZamiaException e) {
				ZamiaPlugin.showError(getSite().getShell(), "Path syntax error", "Failed to parse path\n" + aPathStr, "Path syntax error");
			}

		} else {

			PathName path = new PathName(aPathStr);

			// guess toplevel

			Toplevel tl = null;
			if (fPath != null) {
				tl = fPath.getToplevel();
			} else {

				ZamiaProject zprj = fReconcilingStrategy.getZPrj();

				BuildPath bp = zprj.getBuildPath();

				IGManager igm = zprj.getIGM();

				if (bp != null) {

					for (Toplevel toplevel : bp.toplevels()) {

						IGItem item = igm.findItem(toplevel, path);
						if (item != null) {
							tl = toplevel;
							break;
						}
					}

				}

			}
			if (tl == null) {
				ZamiaPlugin.showError(getSite().getShell(), "Failed to guess toplevel", "Unable to find toplevel for relative path.", "No build path or no matching toplevel.");
				return;
			}

			tlp = new ToplevelPath(tl, path);
		}

		if (tlp != null) {
			IEditorPart editor = ZamiaPlugin.showSource(getSite().getPage(), fReconcilingStrategy.getZPrj(), tlp);

			if (editor == null) {
				ZamiaPlugin.showError(getSite().getShell(), "Unknown Path", "Failed to locate\n" + aPathStr, "Unknown path.");
			}
		}

	}

	public static void updateOutlineView() {
		Display d = Display.getDefault();

		d.asyncExec(new Runnable() {
			public void run() {

				IEditorPart editor = ZamiaPlugin.getPage().getActiveEditor();

				if (editor instanceof ZamiaEditor) {
					ZamiaEditor ze = (ZamiaEditor) editor;
					ze.updateOutlinePage();
				}
			}
		});
	}
}
