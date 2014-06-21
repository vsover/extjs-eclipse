/*******************************************************************************
 * Copyright (c) 2013 w3des.net and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *  
 * Contributors:
 *      w3des.net - initial API and implementation
 ******************************************************************************/
package net.w3des.extjs.ui.preferences;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import net.w3des.extjs.core.api.IExtJSEnvironment;
import net.w3des.extjs.core.api.IExtJSLibrary;
import net.w3des.extjs.internal.core.ExtJSCore;
import net.w3des.extjs.ui.ExtJSUI;
import net.w3des.extjs.ui.common.DialogField;
import net.w3des.extjs.ui.common.ITreeListAdapter;
import net.w3des.extjs.ui.common.LayoutUtil;
import net.w3des.extjs.ui.common.TreeListDialogField;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * 
 */
public class ExtJSEnvironmentPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private TreeListDialogField fEnvList;
	
	private static final int IDX_NEW = 0;
	private static final int IDX_EDIT = 1;
	private static final int IDX_ADD_VERSION = 2;
	private static final int IDX_ADD_LIBRARY = 3;
	private static final int IDX_REMOVE = 4;
	
	private List<EnvElement> removedEnvironments = new ArrayList<EnvElement>();

	public ExtJSEnvironmentPreferencePage() {
		setPreferenceStore(ExtJSUI.getDefault().getPreferenceStore());
		setDescription("ExtJS build environments");
		noDefaultAndApplyButton();
		
		EnvAdapter adapter= new EnvAdapter();
		String[] buttonLabels= new String[] {
				"new", 
				"edit", 
				"add version", 
				"add library", 
				"remove"
		};
		
		this.fEnvList = new TreeListDialogField(adapter, buttonLabels, new EnvListLabelProvider());
		this.fEnvList.setLabelText("Environments");
		
		final IExtJSEnvironment[] envs = ExtJSCore.getLibraryManager().getEnvironments();
		final List<EnvElement> list = new ArrayList<EnvElement>();
		for (final IExtJSEnvironment env : envs) {
			list.add(new EnvElement(env));
		}
		this.fEnvList.setElements(list);
		this.fEnvList.setViewerComparator(new EnvListElementSorter());
		
		doSelectionChanged(this.fEnvList); //update button enable state 
	}
	
	@Override
	public void applyData(Object data) {
		// empty
	}
	
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
	}	

	protected Control createContents(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		LayoutUtil.doDefaultLayout(composite, new DialogField[] { this.fEnvList }, true);
		LayoutUtil.setHorizontalGrabbing(this.fEnvList.getTreeControl(null));
		Dialog.applyDialogFont(composite);
		return composite;
	}
	
	public void init(IWorkbench workbench) {
	}
	
	@Override
	protected void performDefaults() {
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() { 
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						if (monitor != null) {
							monitor= new NullProgressMonitor();
						}
						
						updateEnvironments(monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					} finally {
						monitor.done();
					}
				}
			});
		} catch (InterruptedException e) {
			// cancelled by user
		} catch (InvocationTargetException e) {
			String title = "Error"; 
			String message = "Error applying extjs environment settings";
			ExtJSCore.error(message, e);
			MessageDialog.openError(getShell(), title, message);
		}
		return true;
	}
	
	private void updateEnvironments(IProgressMonitor monitor) throws CoreException {
		// TODO
	}
	
	protected void doSelectionChanged(TreeListDialogField field) {
		List<Object> list = field.getSelectedElements();
		field.enableButton(IDX_REMOVE, canRemove(list));
		field.enableButton(IDX_EDIT, canEdit(list));
		field.enableButton(IDX_ADD_VERSION, canAddVersion(list));
		field.enableButton(IDX_ADD_LIBRARY, canAddLibrary(list));
	}
	
	protected void doCustomButtonPressed(TreeListDialogField field, int index) {
		if (index == IDX_NEW) {
			doAddNew();
		} else if (index == IDX_ADD_VERSION) {
			doAddVersion(field.getSelectedElements());
		} else if (index == IDX_ADD_LIBRARY) {
			doAddLibrary(field.getSelectedElements());
		} else if (index == IDX_REMOVE) {
			doRemove(field.getSelectedElements());
		} else if (index == IDX_EDIT) {
			doEdit(field.getSelectedElements());
		}
	}
	
	protected void doDoubleClicked(TreeListDialogField field) {
		List<Object> selected = field.getSelectedElements();
		if (canEdit(selected)) {
			doEdit(field.getSelectedElements());
		}
	}
	
	protected void doKeyPressed(TreeListDialogField field, KeyEvent event) {
		if (event.character == SWT.DEL && event.stateMask == 0) {
			List<Object> selection = field.getSelectedElements();
			if (canRemove(selection)) {
				doRemove(selection);
			}
		}
	}
	
	private void doEdit(List<Object> selected) {
		if (selected.size() == 1) {
			Object curr = selected.get(0);
			if (curr instanceof EnvElement) {
				editEnvElement((EnvElement) curr);
			}
			doSelectionChanged(this.fEnvList);
		}
	}
	
	private void doAddNew() {
		final EnvDialog dialog = new EnvDialog(getShell(), this.fEnvList.getElements(), null);
		if (dialog.open() == Window.OK) {
			final EnvElement element = new EnvElement(dialog.getName(), dialog.getSelectedVersions());
			switch (dialog.getCoreType()) {
			default:
			case NONE:
				// do nothing
				break;
			case FOLDER:
				element.setCoreFolder(dialog.getCorePath());
				break;
			case ZIP:
				element.setCoreZip(dialog.getCorePath());
				break;
			}
			fEnvList.addElement(element);
		}
	}

	private void editEnvElement(EnvElement element) {
		final EnvDialog dialog = new EnvDialog(getShell(), this.fEnvList.getElements(), element);
		if (dialog.open() == Window.OK) {
			element.setName(dialog.getName());
			element.setVersions(dialog.getSelectedVersions());
			switch (dialog.getCoreType()) {
			default:
			case NONE:
				element.setCoreNone();
				break;
			case FOLDER:
				element.setCoreFolder(dialog.getCorePath());
				break;
			case ZIP:
				element.setCoreZip(dialog.getCorePath());
				break;
			}
			fEnvList.refresh(element);
		}
	}

	private void doRemove(List<Object> selected) {
		Object selectionAfter= null;
		for (int i= 0; i < selected.size(); i++) {
			Object curr = selected.get(i);
			if (curr instanceof EnvElement) {
				if (!((EnvElement) curr).isNew()) {
					this.removedEnvironments.add((EnvElement) curr);
					((EnvElement) curr).delete();
				}
				this.fEnvList.removeElement(curr);
			}
			else if (curr instanceof EnvLibElement) {
				final EnvElement parent = ((EnvLibElement) curr).getParent();
				((EnvLibElement) curr).delete();
				parent.remove((EnvLibElement) curr);
				this.fEnvList.removeElement(curr);
				selectionAfter = parent;
			}
			else if (curr instanceof EnvVersionElement) {
				final EnvElement parent = ((EnvLibElement) curr).getParent();
				((EnvVersionElement) curr).delete();
				parent.remove((EnvVersionElement) curr);
				this.fEnvList.removeElement(curr);
				selectionAfter = parent;
			}
		}
		if (this.fEnvList.getSelectedElements().isEmpty()) {
			if (selectionAfter != null) {
				this.fEnvList.selectElements(new StructuredSelection(selectionAfter));
			} else {
				this.fEnvList.selectFirstElement();	
			}
		} else {
			doSelectionChanged(this.fEnvList);
		}
	}

	private void doAddVersion(List<Object> list) {
		if (canAddVersion(list)) {
			final EnvElement env = (EnvElement) list.get(0);
			final IProjectFacet facet = ProjectFacetsManager.getProjectFacet(ExtJSCore.FACET_EXT);
			final List<IProjectFacetVersion> facets = new ArrayList<IProjectFacetVersion>(facet.getVersions());
			final List<String> newVersions = new ArrayList<String>();
			for (final Object child : env.getChildren())
			{
				if (child instanceof EnvVersionElement)
				{
					facets.remove(facet.getVersion(((EnvVersionElement) child).getName()));
					newVersions.add(((EnvVersionElement) child).getName());
				}
			}
			final ChooseVersionDialog dialog = new ChooseVersionDialog(getShell(), facets);
			if (dialog.open() == Window.OK) {
				for (final String v : dialog.getSelectedVersions()) {
					newVersions.add(v);
				}
				env.setVersions(newVersions.toArray(new String[newVersions.size()]));
				fEnvList.refresh(env);
			}
		}
	}

	private void doAddLibrary(List<Object> list) {
		if (canAddLibrary(list)) {
			final EnvElement env = (EnvElement) list.get(0);
			final List<IExtJSLibrary> libs = new ArrayList<IExtJSLibrary>();
			for (final IExtJSLibrary lib : ExtJSCore.getLibraryManager().getLibraries()) {
				libs.add(lib);
			}
			for (final Object child : env.getChildren())
			{
				if (child instanceof EnvLibElement)
				{
					libs.remove(ExtJSCore.getLibraryManager().getLibrary(((EnvLibElement) child).getName()));
				}
			}
			final ChooseLibraryDialog dialog = new ChooseLibraryDialog(getShell(), libs);
			if (dialog.open() == Window.OK) {
				for (final String libName : dialog.getSelectedLibraries()) {
					env.addLibrary(libName);
				}
				fEnvList.refresh(env);
			}
		}
	}
	
	private boolean canAddVersion(List<Object> list) {
		if (list.size() != 1)
			return false;
		final Object object = list.get(0);
		if (object instanceof EnvElement)
		{
			final IExtJSEnvironment env = ((EnvElement)object).getEnvironment();
			return env == null || !env.isBuiltin();
		}
		return false;
	}
	
	private boolean canAddLibrary(List<Object> list) {
		if (list.size() != 1)
			return false;
		return list.get(0) instanceof EnvElement;
	}

	private boolean canEdit(List<Object> list) {
		if (list.size() != 1)
			return false;
		
		final Object obj = list.get(0);
		if (obj instanceof EnvVersionElement)
			return false;
		if (obj instanceof EnvLibElement)
			return false;
		
		return true;
	}

	private boolean canRemove(List<Object> list) { 
		if (list.size() == 0) {
			return false;
		}
		for (int i= 0; i < list.size(); i++) {
			final Object elem = list.get(i);
			if (elem instanceof EnvElement) {
				final IExtJSEnvironment environment = ((EnvElement) elem).getEnvironment();
				if (environment != null && environment.isBuiltin())
					return false;
			}
			if (elem instanceof EnvVersionElement) {
				final IExtJSEnvironment environment = ((EnvVersionElement) elem).getEnvironment();
				if (environment != null && environment.isBuiltin())
					return false;
			}
		}
		return true;
	}

	
	private class EnvAdapter implements ITreeListAdapter {
		
		private final Object[] EMPTY= new Object[0];

		public void customButtonPressed(TreeListDialogField field, int index) {
			doCustomButtonPressed(field, index);
		}

		public void selectionChanged(TreeListDialogField field) {
			doSelectionChanged(field);
		}
		
		public void doubleClicked(TreeListDialogField field) {
			doDoubleClicked(field);
		}

		public void keyPressed(TreeListDialogField field, KeyEvent event) {
			doKeyPressed(field, event);
		}

		public Object[] getChildren(TreeListDialogField field, Object element) {
			if (element instanceof EnvElement) {
				EnvElement elem = (EnvElement) element;
				return elem.getChildren();
			}
			return EMPTY;
		}

		public Object getParent(TreeListDialogField field, Object element) {
			if (element instanceof EnvLibElement) {
				return ((EnvLibElement) element).getParent();
			} else if (element instanceof EnvVersionElement) {
				return ((EnvVersionElement) element).getParent();
			} 
			return null;
		}

		public boolean hasChildren(TreeListDialogField field, Object element) {
			return getChildren(field, element).length > 0;
		}
				
	}
	
}