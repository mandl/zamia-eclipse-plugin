/*******************************************************************************
 * Copyright (c) 2004, 2006 KOBAYASHI Tadashi and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ali Ghorashi - Initial Implementation
 *******************************************************************************/
package org.zamia.plugin.preferences;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.texteditor.templates.TemplatePreferencePage;
import org.zamia.plugin.ZamiaPlugin;



/**
 * Templates preference
 */
public class ZamiaTemplatePreferencePage extends TemplatePreferencePage
{
	public ZamiaTemplatePreferencePage(){
		super();
		//HdlTextAttribute.init();
		setTemplateStore(ZamiaPlugin.getPlugin().getTemplateStore());
		setContextTypeRegistry(ZamiaPlugin.getPlugin().getContextTypeRegistry());		
	}
	
	/**
	 * Returns whether the formatter preference checkbox should be shown.
	 *
	 * @return <code>true</code> if the formatter preference checkbox should
	 *         be shown, <code>false</code> otherwise
	 */
	protected boolean isShowFormatterSetting() {
		return false;
	}
	
	/**
	 * Creates, configures and returns a source viewer to present the template
	 * pattern on the preference page. Clients may override to provide a custom
	 * source viewer featuring e.g. syntax coloring.
	 *
	 * @param parent the parent control
	 * @return a configured source viewer
	 */
	protected SourceViewer createViewer(Composite parent) {
		SourceViewer viewer= new SourceViewer(parent, null, null, false, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		SourceViewerConfiguration configuration= new SourceViewerConfiguration();
		viewer.configure(configuration);
		IDocument document= new Document();
		viewer.setDocument(document);
		return viewer;
	}
	
	/**
	 * Updates the pattern viewer.
	 */
	protected void updateViewerInput() {
		IStructuredSelection selection= (IStructuredSelection) getTableViewer().getSelection();

		if (selection.size() == 1) {
			TemplatePersistenceData data= (TemplatePersistenceData) selection.getFirstElement();
			Template template= data.getTemplate();
			if(template.getContextTypeId().contains("vhdl"))
			{
				getViewer().unconfigure();
				//SourceViewerConfiguration configuration= HdlSourceViewerConfiguration.createForVhdl(new VerilogEditor());
				//getViewer().configure(configuration);
							
			}

			else
			{
				getViewer().unconfigure();
				SourceViewerConfiguration configuration= new SourceViewerConfiguration();
				getViewer().configure(configuration);
			}
		} 
		super.updateViewerInput();
	}
}

