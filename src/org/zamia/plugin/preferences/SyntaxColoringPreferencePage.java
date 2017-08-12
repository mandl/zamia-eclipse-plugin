/* 
 * Copyright 2008,2011 by the authors indicated in the @author tags. 
 * All rights reserved. 
 * 
 * See the LICENSE file for details.
 */
package org.zamia.plugin.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.zamia.plugin.ZamiaPlugin;
import org.zamia.plugin.editors.ZamiaEditor;


/**
 * 
 * @author Guenter Bartsch
 * 
 */
public class SyntaxColoringPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public SyntaxColoringPreferencePage() {
		super(GRID);
		setPreferenceStore(ZamiaPlugin.getDefault().getPreferenceStore());
		setDescription("zamiaCAD");
	}

	public void createFieldEditors() {

		addField(new ColorFieldEditor(PreferenceConstants.P_KEYWORD, "&Keyword color", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_STRING, "&String color", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_COMMENT, "&Comment color", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_DEFAULT, "&Default text color", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_MATCHING_CHAR, "&Matching char color", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_BACKGROUND, "&Background color", getFieldEditorParent()));

		addField(new ColorFieldEditor(PreferenceConstants.P_MODULE, "&Module color", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_SIGNAL, "S&ignal color", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_MODULE_LABEL, "Module &label color", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_HILIGHT, "&Hilight color", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.P_MARKER_LABEL,"Show maker &time",getFieldEditorParent()));
		addField(new DirectoryFieldEditor(PreferenceConstants.P_PYTHON_PATH,"Set &python path",getFieldEditorParent()));
		
	}

	protected void performApply() {
		updateColors();
		super.performApply();
	}

	public boolean performOk() {
		updateColors();
		return super.performOk();
	}

	protected void updateColors() {
		IWorkbenchPage page = ZamiaPlugin.getPage();
		if (page != null) {
			IEditorReference[] editorReference = page.getEditorReferences();
			for (int i = 0; i < editorReference.length; i++) {
				IEditorPart editorPart = editorReference[i].getEditor(false);
				if (editorPart instanceof ZamiaEditor) {
					ZamiaEditor editor = (ZamiaEditor) editorPart;
					editor.updateColors();
				}
			}
		}
	}

	public void init(IWorkbench workbench) {
	}

}