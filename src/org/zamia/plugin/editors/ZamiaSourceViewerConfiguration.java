/*
 * Copyright 2007-2008 by the authors indicated in the @author tags.
 * All rights reserved.
 *
 * See the LICENSE file for details.
 *
 * 
 */
package org.zamia.plugin.editors;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.ITextEditor;
import org.zamia.plugin.editors.buildpath.BasicViewerConfiguration;
import org.zamia.plugin.editors.completion.VHDLCompletionProcessor;


/**
 * 
 * @author Guenter Bartsch
 *
 */

public class ZamiaSourceViewerConfiguration extends BasicViewerConfiguration {

	private ZamiaReconcilingStrategy strategy;
	
	public ZamiaSourceViewerConfiguration(BasicIdentifierScanner scanner_, ZamiaReconcilingStrategy strategy_, String[] defaultPrefixes_, ITextEditor editor_) {
		super(scanner_, defaultPrefixes_, editor_);
		strategy = strategy_;
	}

	private IInformationControlCreator getInformationPresenterControlCreator(ISourceViewer sourceViewer) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
//				return new DefaultInformationControl(parent, true);
				return new DefaultInformationControl(parent);
			}
		};
	}

	
	@Override
	public IInformationPresenter getInformationPresenter(ISourceViewer sourceViewer) {
		InformationPresenter presenter= new InformationPresenter(getInformationPresenterControlCreator(sourceViewer));
		presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		
		// Register information provider
		IInformationProvider provider= createInfoProvider();
		String[] contentTypes= getConfiguredContentTypes(sourceViewer);
		for (int i= 0; i < contentTypes.length; i++)
			presenter.setInformationProvider(provider, contentTypes[i]);
		
		// sizes: see org.eclipse.jface.text.TextViewer.TEXT_HOVER_*_CHARS
		presenter.setSizeConstraints(100, 12, true, true);
		return presenter;
	}

	private VHDLInformationProvider createInfoProvider() {
		ITextEditor editor = getEditor();
		return (editor instanceof VHDLEditor) ? new VHDLInformationProvider(editor) : null ;
	}

	@Override
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		MonoReconciler reconciler = new MonoReconciler(strategy, false);
		reconciler.setProgressMonitor(new NullProgressMonitor());
		reconciler.setDelay(500);
        reconciler.setIsIncrementalReconciler(false);

		return reconciler;
	}

	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {

		ContentAssistant assistant= new ContentAssistant();
		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		assistant.setContentAssistProcessor(new VHDLCompletionProcessor(getEditor()), IDocument.DEFAULT_CONTENT_TYPE);
	
		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(500);
		assistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		assistant.setContextInformationPopupBackground(ColorManager.getInstance().getColor(new RGB(150, 150, 0)));

		return assistant;
	}

	@Override
	public VHDLInformationProvider getTextHover(ISourceViewer sourceViewer, String contentType) {
		return createInfoProvider();
	}
	
	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
		throw new RuntimeException("getTextHover(viewer, contentType, stateMask) is not supported");
	}
	
	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
	    return new IHyperlinkDetector[] { new HyperlinkDetector()
	    	, new org.eclipse.jface.text.hyperlink.URLHyperlinkDetector() 
	    };
	}

}
