/* 
 * Copyright 2009 by the authors indicated in the @author tags. 
 * All rights reserved. 
 * 
 * See the LICENSE file for details.
 * 
 * Created by Guenter Bartsch on Apr 2, 2009
 */
package org.zamia.plugin.views.navigator;

import java.util.ArrayList;
import java.util.Collections;

import org.zamia.SourceLocation;
import org.zamia.ToplevelPath;
import org.zamia.ZamiaProject;
import org.zamia.instgraph.IGConcurrentStatement;
import org.zamia.instgraph.IGContainer;
import org.zamia.instgraph.IGContainerItem;
import org.zamia.instgraph.IGInstantiation;
import org.zamia.instgraph.IGItem;
import org.zamia.instgraph.IGManager;
import org.zamia.instgraph.IGModule;
import org.zamia.instgraph.IGObject;
import org.zamia.instgraph.IGOperation;
import org.zamia.instgraph.IGPackageImport;
import org.zamia.instgraph.IGProcess;
import org.zamia.instgraph.IGStructure;
import org.zamia.instgraph.IGType;
import org.zamia.util.HashSetArray;
import org.zamia.util.PathName;
import org.zamia.vhdl.ast.DMUID;


/**
 * Blue IG
 * 
 * @author Guenter Bartsch
 * 
 */

public class IGModuleWrapper implements Comparable<IGModuleWrapper> {

	public enum IGMWOp {
		BLUEIG, TOPRED, STRUCTURE, LOCALS, INSTANTIATION, ITEM, GLOBALS, PROCESS
	};

	private IGMWOp fOp;

	private DMUID fDMUID;

	private String fSignature;

	private ToplevelPath fPath;

	private IGInstantiation fInstantiation;

	private ZamiaProject fZPrj;

	private IGModule fModule;

	private IGStructure fStruct;

	private IGContainerItem fItem;

	private IGProcess fProcess;

	private NavigatorWrapperCache fCache;

	public IGModuleWrapper(IGMWOp aOp, String aSignature, DMUID aDMUID, ToplevelPath aPath, NavigatorWrapperCache aCache) {
		fSignature = aSignature;
		fDMUID = aDMUID;
		fPath = aPath;
		fOp = aOp;
		fCache = aCache;
		fZPrj = fCache.getZPrj();
	}

	public IGModuleWrapper(IGInstantiation aInstantiation, ToplevelPath aPath, NavigatorWrapperCache aCache) {
		fInstantiation = aInstantiation;
		fPath = aPath;
		fOp = IGMWOp.INSTANTIATION;
		fCache = aCache;
		fZPrj = fCache.getZPrj();
	}

	public IGModuleWrapper(IGProcess aProcess, ToplevelPath aPath, NavigatorWrapperCache aCache) {
		fProcess = aProcess;
		fPath = aPath;
		fOp = IGMWOp.PROCESS;
		fCache = aCache;
		fZPrj = fCache.getZPrj();
	}

	public IGModuleWrapper(IGMWOp aOp, IGModule aModule, ToplevelPath aPath, NavigatorWrapperCache aCache) {
		fModule = aModule;
		fPath = aPath;
		fOp = aOp;
		fCache = aCache;
		fZPrj = fCache.getZPrj();
	}

	public IGModuleWrapper(IGStructure aStruct, ToplevelPath aTp, NavigatorWrapperCache aCache) {
		fStruct = aStruct;
		fPath = aTp;
		fCache = aCache;
		fZPrj = fCache.getZPrj();
		fOp = IGMWOp.STRUCTURE;
	}

	public IGModuleWrapper(IGContainerItem aItem, ToplevelPath aPath, NavigatorWrapperCache aCache) {
		fItem = aItem;
		fPath = aPath;
		fCache = aCache;
		fZPrj = fCache.getZPrj();
		fOp = IGMWOp.ITEM;
	}

	public IGInstantiation getInstantiation() {
		return fInstantiation;
	}

	public ToplevelPath getPath() {
		return fPath;
	}

	public IGMWOp getOp() {
		return fOp;
	}

	@Override
	public boolean equals(Object aObject) {

		if (!(aObject instanceof IGModuleWrapper)) {
			return false;
		}

		if (!(fOp == IGMWOp.BLUEIG)) {
			return aObject == this;
		}

		IGModuleWrapper wrapper2 = (IGModuleWrapper) aObject;

		if (getOp() != wrapper2.getOp())
			return false;

		if (getOp() != IGMWOp.BLUEIG) {
			ToplevelPath p1 = getPath();
			ToplevelPath p2 = wrapper2.getPath();
			if (p1 != null && p2 != null && !p1.equals(p2)) {
				return false;
			}
			if (p1 == null && p2 != null)
				return false;
			if (p1 != null && p2 == null)
				return false;
		}

		return getDMUID().equals(wrapper2.getDMUID());
	}

	@Override
	public int hashCode() {
		return fOp == IGMWOp.BLUEIG ? getDMUID().hashCode() : super.hashCode();
	}

	public DMUID getDMUID() {
		return fDMUID;
	}

	public void setDMUID(DMUID aDMUID) {
		fDMUID = aDMUID;
	}

	public void setZPrj(ZamiaProject aZPrj) {
		fZPrj = aZPrj;
	}

	public ZamiaProject getZPrj() {
		return fZPrj;
	}

	public Object[] getChildren() {

		IGManager igm = fZPrj.getIGM();

		switch (fOp) {

		case TOPRED:

			ArrayList<IGModuleWrapper> res = new ArrayList<IGModuleWrapper>();
			IGModule module = igm.findModule(fPath.getToplevel());

			if (module != null) {

				res.add(fCache.getLocalsWrapper(module, fPath));
				res.add(fCache.getGlobalsWrapper(module, fPath));

				IGStructure structure = module.getStructure();

				ArrayList<IGModuleWrapper> subs = getChildren(structure);

				Collections.sort(subs);

				int n = subs.size();
				for (int i = 0; i < n; i++) {
					res.add(subs.get(i));
				}
			}

			return res.toArray();

		case INSTANTIATION:
			res = new ArrayList<IGModuleWrapper>();

			module = fInstantiation.findModule();

			if (module != null) {

				res.add(fCache.getLocalsWrapper(module, fPath));
				res.add(fCache.getGlobalsWrapper(module, fPath));

				IGStructure structure = module.getStructure();

				ArrayList<IGModuleWrapper> subs = getChildren(structure);

				Collections.sort(subs);

				int n = subs.size();
				for (int i = 0; i < n; i++) {
					res.add(subs.get(i));
				}
			}

			return res.toArray();

		case STRUCTURE:
			res = getChildren(fStruct);
			return res.toArray();

		case LOCALS:
			res = getChildren(true, fModule.getStructure().getContainer());

			return res.toArray();

		case GLOBALS:

			res = getChildren(false, fModule.getStructure().getContainer());

			return res.toArray();

		case PROCESS:
			res = getChildren(true, fProcess.getContainer());

			return res.toArray();

		case BLUEIG:

			HashSetArray<IGModuleWrapper> wrappers = new HashSetArray<IGModuleWrapper>();

			module = igm.findModule(fSignature);
			if (module != null) {
				findInstantiatedDUs(null, module.getStructure(), wrappers);

				int n = wrappers.size();
				res = new ArrayList<IGModuleWrapper>(n);
				for (int i = 0; i < n; i++) {
					IGModuleWrapper wrapper = wrappers.get(i);
					res.add(wrapper);
				}

				Collections.sort(res);

				return res.toArray();
			}
		default:
			break;

		}

		HashSetArray<Object> res = new HashSetArray<Object>();

		return res.toArray();

	}

	private ArrayList<IGModuleWrapper> getChildren(boolean aLocals, IGContainer aContainer) {

		ArrayList<IGModuleWrapper> res = new ArrayList<IGModuleWrapper>();

		if (aContainer != null) {
			for (IGContainerItem item : aContainer.localItems()) {

				if (isLocal(item) == aLocals) {
					ToplevelPath tp = fPath.append(item.getId());
					res.add(new IGModuleWrapper(item, tp, fCache));
				}
			}

			if (!aLocals) {
				int n = aContainer.getNumPackageImports();
				for (int i = 0; i < n; i++) {
					IGPackageImport pi = aContainer.getPackageImport(i);
					res.add(new IGModuleWrapper(pi, fPath, fCache));
				}
			}
		}

		Collections.sort(res);
		return res;
	}

	private void findInstantiatedDUs(PathName aPrefix, IGStructure aStructure, HashSetArray<IGModuleWrapper> aWrappers) {

		ToplevelPath basePath = aPrefix != null ? fPath.append(aPrefix) : fPath;

		for (IGConcurrentStatement stmt : aStructure.getStatements()) {

			if (stmt instanceof IGInstantiation) {

				IGInstantiation inst = (IGInstantiation) stmt;

				ToplevelPath path = basePath.append(inst.getLabel());

				IGModuleWrapper wrapper = fCache.getBlueWrapper(inst, path);

				aWrappers.add(wrapper);

			} else if (stmt instanceof IGStructure) {

				IGStructure struct = (IGStructure) stmt;

				findInstantiatedDUs(aPrefix != null ? aPrefix.append(struct.getLabel()) : new PathName(struct.getLabel()), struct, aWrappers);

			}
		}
	}

	private boolean isLocal(IGItem aItem) {
		return (aItem instanceof IGObject) || (aItem instanceof IGType) || (aItem instanceof IGProcess);
	}

	public boolean isBlueIG() {
		return fOp == IGMWOp.BLUEIG;
	}

	private ArrayList<IGModuleWrapper> getChildren(IGStructure aStructure) {

		ArrayList<IGModuleWrapper> res = new ArrayList<IGModuleWrapper>(aStructure.getNumStatements());

		for (IGConcurrentStatement stmt : aStructure.getStatements()) {

			if (stmt instanceof IGInstantiation) {

				IGInstantiation inst = (IGInstantiation) stmt;

				ToplevelPath tp = fPath.append(inst.getLabel());

				res.add(fCache.getInstantiationWrapper(inst, tp));

			} else if (stmt instanceof IGStructure) {

				IGStructure struct = (IGStructure) stmt;

				ToplevelPath tp = fPath.append(struct.getLabel());

				res.add(fCache.getStructureWrapper(struct, tp));

			} else if (stmt instanceof IGProcess) {

				IGProcess proc = (IGProcess) stmt;

				if (proc.getLabel() != null) {
					ToplevelPath tp = fPath.append(proc.getLabel());
					res.add(fCache.getProcessWrapper(proc, tp));
				}
			}
		}

		// int n = module.getNumInstantiations();
		//
		// for (int i = 0; i < n; i++) {
		//
		// IGInstantiation inst = module.getInstantiation(i);
		//
		// DUUID childDUUID = inst.getChildDUUID();
		//
		// if (!wrapper.isBlueIG()) {
		// res.add(new IGModuleWrapper(childDUUID, inst,
		// path.append(inst.getLabel()), zprj, false));
		// } else {
		// res.add(new IGModuleWrapper(childDUUID,
		// path.append(inst.getLabel()), zprj, true));
		// }
		// }

		return res;
	}

	@Override
	public String toString() {

		String label = "???";

		switch (fOp) {
		case TOPRED:
			label = fDMUID.toCompactString();
			break;

		case INSTANTIATION:
			label = fInstantiation.getLabel() + ": " + fInstantiation.getChildDUUID().toCompactString();
			break;

		case PROCESS:
			label = fProcess.getLabel();
			if (label == null)
				label = "Anonymous process";
			break;

		case LOCALS:
			label = "Locals";
			break;

		case GLOBALS:
			label = "Globals";
			break;

		case BLUEIG:
			label = fDMUID.toCompactString();
			// if (!wrapper.isBlueIG()) {
			// ToplevelPath tlp = wrapper.getPath();
			// PathName path = tlp.getPath();
			//
			// if (path.getNumSegments() > 0) {
			// label = path.getSegment(path.getNumSegments() - 1) + ": ";
			// }
			// }
			break;

		case STRUCTURE:
			label = fStruct.getLabel();
			break;

		case ITEM:

			label = fItem.getId();

			if (fItem instanceof IGObject) {
				IGObject obj = (IGObject) fItem;

				IGType type = obj.getType();

				if (type != null) {
					label = label + ": " + type.toHRString();
				}

				IGOperation iv = obj.getInitialValue();
				if (iv != null) {
					label = label + " = " + iv.toString();
				}
			}

			break;
		}

		// FIXME
		return label;
	}

	@Override
	public int compareTo(IGModuleWrapper aO) {
		String s1 = toString();
		String s2 = aO.toString();
		return s1.compareTo(s2);
	}

	public boolean hasChildren() {
		IGModule module;
		switch (fOp) {
		case LOCALS:
			return true;
		case BLUEIG:
			
			HashSetArray<IGModuleWrapper> wrappers = new HashSetArray<IGModuleWrapper>();
			IGManager igm = fZPrj.getIGM();
			module = igm.findModule(fSignature);
			if (module != null) {
				findInstantiatedDUs(null, module.getStructure(), wrappers);

				if(wrappers.size() == 0)
					return false;
				return true;
			}
			break;
		case ITEM:
			return false;
		default:
		 
			break;
		}

		// FIXME
		return true;
	}

	public IGItem getItem() {
		return fItem;
	}

	public SourceLocation getLocation() {

		SourceLocation location = null;

		IGManager igm = fZPrj.getIGM();

		IGModule module;
		switch (fOp) {
		case BLUEIG:
			IGItem item = igm.findItem(fPath.getToplevel(), fPath.getPath());
			if (item != null) {
				location = item.computeSourceLocation();
			}
			break;

		case ITEM:
			location = fItem.computeSourceLocation();
			break;

		case INSTANTIATION:
			location = fInstantiation.computeSourceLocation();
			break;

		case GLOBALS:
		case LOCALS:
			break;

		case STRUCTURE:
			location = fStruct.computeSourceLocation();
			break;

		case TOPRED:
			module = igm.findModule(fPath.getToplevel());
			location = module.computeSourceLocation();
			break;

		case PROCESS:
			location = fProcess.computeSourceLocation();
			break;

		}

		return location;
	}

	public IGModule getModule() {
		return fModule;
	}

	public IGStructure getStruct() {
		return fStruct;
	}

	public IGProcess getProcess() {
		return fProcess;
	}

	public ToplevelPath getEditorPath() {
		switch (fOp) {
		case BLUEIG:
		case INSTANTIATION:
			if (fPath != null) {
				return fPath.getNullParent();
			}
			break;
		default:
			break;
		}
		return getPath();
	}

}
