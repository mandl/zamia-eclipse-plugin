package org.zamia.plugin.editors;

import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Composite;
import org.zamia.plugin.editors.buildpath.BasicViewerConfiguration;
import org.zamia.plugin.preferences.PreferenceConstants;

public class UCFEditor extends ErrorMarkEditor {
	
	public static class UCFConfiguration extends BasicViewerConfiguration {

		static class Scanner extends BasicIdentifierScanner {

			@Override
			public void addStrComment(List<IRule> rules, IToken string, IToken comment) {
			
				rules.add(new EndOfLineRule("#",token(PreferenceConstants.P_COMMENT)));
			}
			
			public boolean ignoreCase() {return true;}

			public String[] getKeywords() {
				return new String[] {"NET","LOC","IOSTANDARD","SLEW","DRIVE","CONFIG","PROHIBIT","PERIOD"};
			}

			

			
		}
		
		public UCFConfiguration(UCFEditor editor) {
			super(new Scanner(), new String[] {"#", ""}, editor);
		}
		
		public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
			return new String[] {IDocument.DEFAULT_CONTENT_TYPE,};
		}

		public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
			PresentationReconciler reconciler = new PresentationReconciler();

			DefaultDamagerRepairer dr = new DefaultDamagerRepairer(fScanner);
			reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
			reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
			
			return reconciler;
		}
		
	}
	
	public UCFEditor() {
		setSourceViewerConfiguration(new UCFConfiguration(this));
	}

	
}


