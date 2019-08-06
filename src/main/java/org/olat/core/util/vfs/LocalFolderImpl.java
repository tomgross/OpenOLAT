/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.  
* <p>
*/ 

package org.olat.core.util.vfs;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.FileUtils;
import org.olat.core.util.vfs.filters.VFSItemFilter;
import org.olat.core.util.vfs.version.Versionable;
import org.olat.core.util.vfs.version.VersionsManager;

/**
 * Description:<br>
 * VFSContainer implementation that is based on a java.io.File from a local
 * filesystem. It implements a regular directory
 * 
 * <P>
 * Initial Date: 23.06.2005 <br>
 * 
 * @author Felix Jost
 */
public class LocalFolderImpl extends LocalImpl implements VFSContainer {
	private static final OLog log = Tracing.createLoggerFor(LocalFolderImpl.class);

	private VFSItemFilter defaultFilter=null;
	
	/**
	 * @param folderfile
	 */
	private LocalFolderImpl() {
		super(null, null);
		throw new AssertException("Cannot instantiate LocalFolderImpl().");
	}
	
	/**
	 * Constructor
	 * @param folderFile The real file of type directory wrapped by this VFSContainer
	 */
	public LocalFolderImpl(File folderFile) {
		this(folderFile, null);
	}
	
	/**
	 * @param folderfile
	 */
	protected LocalFolderImpl(File folderfile, VFSContainer parent) {
		super(folderfile, parent);
		boolean alreadyExists = folderfile.exists();
		boolean succesfullCreated = alreadyExists ? true : folderfile.mkdirs();
		//check against concurrent creation of the folder, mkdirs return false if the directory exists
		if (!alreadyExists && !succesfullCreated && folderfile.exists()) {
			succesfullCreated = true;
		}
		if (!alreadyExists && !succesfullCreated) {
			throw new AssertException("Cannot create directory of LocalFolderImpl with reason (exists= ): "+alreadyExists+" && created= "+succesfullCreated+") path: " + folderfile.getAbsolutePath());
		}
	}
	
	/**
	 * @see org.olat.core.util.vfs.VFSContainer#getItems()
	 */
	public List<VFSItem> getItems() {
		return getItems(null);
	}

	/**
	 * @see org.olat.core.util.vfs.VFSContainer#getItems(org.olat.core.util.vfs.filters.VFSItemFilter)
	 */
	@Override
	public List<VFSItem> getItems(VFSItemFilter filter) {
		File aFolder = getBasefile();
		if(! aFolder.isDirectory()){
			throw new AssertException("basefile is not a directory: "+aFolder.getAbsolutePath());			
		}
		File[] children = aFolder.listFiles();
		if(children == null) {
			children = new File[0];
		}
		int len = children.length;
		List<VFSItem> res = new ArrayList<VFSItem>(len);

		for (int i = 0; i < len; i++) {
			File af = children[i];
			VFSItem item;
			if (af.isDirectory()) {
				LocalFolderImpl folderItem = new LocalFolderImpl(af, this);
				folderItem.setDefaultItemFilter(defaultFilter);
				item = folderItem;
			} else {
				item = new LocalFileImpl(af, this);
			}
			if (defaultFilter == null){
				if (filter == null) res.add(item);
				else if (filter.accept(item)) res.add(item);
				
			} else if (defaultFilter.accept(item)){
				if (filter == null ) res.add(item);
				else if (filter.accept(item) ) res.add(item);
			}
		}
		return res;
	}

	/**
	 * @see org.olat.core.util.vfs.VFSContainer#setDefaultItemFilter(org.olat.core.util.vfs.filters.VFSItemFilter)
	 */
	public void setDefaultItemFilter(VFSItemFilter defaultFilter){
		this.defaultFilter = defaultFilter;
	}
	
	/**
	 * @see org.olat.core.util.vfs.VFSContainer#copyFrom(org.olat.core.util.vfs.VFSItem)
	 */
	public VFSStatus copyFrom(VFSItem source) {
		return copyFrom(source, true);
	}
	
	/**
	 * Internal copy from, preventing quota checks on subfolders.
	 * 
	 * @param source
	 * @param checkQuota
	 * @return
	 */
	private VFSStatus copyFrom(VFSItem source, boolean checkQuota) {

		if (source.canCopy() != VFSConstants.YES) throw new RuntimeException("cannot copy from");
		
		String sourcename = source.getName();
		File basefile = getBasefile();

		// check if there is already an item with the same name...
		if (resolve(sourcename) != null)
			return VFSConstants.ERROR_NAME_ALREDY_USED;
		
		// add either file bla.txt or folder blu as a child of this folder
		if (source instanceof VFSContainer) {
			// copy recursively
			VFSContainer sourcecontainer = (VFSContainer)source;
			// check if this is a containing container...
			if (VFSManager.isSelfOrParent(sourcecontainer, this))
				return VFSConstants.ERROR_OVERLAPPING;
			
			// "copy" the container means creating a folder with that name
			// and let the children copy

			// create the folder
			File outdir = new File(basefile, sourcename);
			outdir.mkdir();
			LocalFolderImpl rootcopyfolder = new LocalFolderImpl(outdir, this);

			List<VFSItem> children = sourcecontainer.getItems();
			for (VFSItem chd:children) {
				VFSStatus status = rootcopyfolder.copyFrom(chd, false);
				if (status != VFSConstants.SUCCESS) return status;
			}
		} else if (source instanceof VFSLeaf) {
			// copy single item
			VFSLeaf s = (VFSLeaf) source;
			// check quota
			if (checkQuota) {
				long quotaLeft = VFSManager.getQuotaLeftKB(this);
				if(quotaLeft != Quota.UNLIMITED && quotaLeft < (s.getSize() / 1024))
					return VFSConstants.ERROR_QUOTA_EXCEEDED;
			}
			
			try {
				FileUtils.bcopy(s.getInputStream(), new File(basefile, sourcename), "VFScopyFrom");
			} catch (Exception e) {
				return VFSConstants.ERROR_FAILED;
			}
			
			if(s instanceof Versionable && ((Versionable)s).getVersions().isVersioned()) {
				((Versionable)s).getVersions().move(this);
			}

		} else throw new RuntimeException("neither a leaf nor a container!");
		return VFSConstants.SUCCESS;
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#canWrite()
	 */
	public VFSStatus canWrite() {
		VFSContainer inheritingContainer = VFSManager.findInheritingSecurityCallbackContainer(this);
		if (inheritingContainer != null && !inheritingContainer.getLocalSecurityCallback().canWrite())
			return VFSConstants.NO_SECURITY_DENIED;
		return VFSConstants.YES;
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#rename(java.lang.String)
	 */
	public VFSStatus rename(String newname) {
		File f = getBasefile();
		File par = f.getParentFile();
		File nf = new File(par, newname);
		VersionsManager.getInstance().rename(this, newname);
		boolean ren = f.renameTo(nf);
		if (ren) {
			// f.renameTo() does NOT modify the path contained in the object f!!
			// The guys at sun consider this a feature and not a bug...
			// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4094022
			// We need to manually reload the new basefile and set it in our parent
			super.setBasefile(new File(nf.getAbsolutePath()));
			return VFSConstants.YES; 
		} else {
			return VFSConstants.NO;
		}
		// FIXME notify children that a parent has been renamed?
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#delete()
	 */
	public VFSStatus delete() {
		if(!getBasefile().exists()){
			return VFSConstants.YES;  // already non-existent
		}
		// we must empty the folders and subfolders first
		for (Iterator<VFSItem> it_chd = getItems().iterator(); it_chd.hasNext();) {
			it_chd.next().delete(); // TODO status
		}
		
		VersionsManager.getInstance().delete(this, false);

		// now delete the directory itself
		boolean del = getBasefile().delete();
		return del ? VFSConstants.YES : VFSConstants.NO;
	}
	
	

	@Override
	public VFSStatus deleteSilently() {
		if(!getBasefile().exists()){
			return VFSConstants.YES;  // already non-existent
		}
		// we must empty the folders and subfolders first
		for (Iterator<VFSItem> it_chd = getItems().iterator(); it_chd.hasNext();) {
			it_chd.next().deleteSilently(); // TODO status
		}
		
		VersionsManager.getInstance().delete(this, true);

		// now delete the directory itself
		boolean del = getBasefile().delete();
		return del ? VFSConstants.YES : VFSConstants.NO;
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#resolveFile(java.lang.String)
	 */
	public VFSItem resolve(String path) {
		VFSItem resolved = VFSManager.resolveFile(this, path);
		// set default filter on resolved file if it is a container
		if (resolved != null && resolved instanceof VFSContainer) {
			VFSContainer resolvedContainer = (VFSContainer) resolved;
			resolvedContainer.setDefaultItemFilter(defaultFilter);
		}
		return resolved;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "LFolder [base="+getBasefile()+"] ";
	}

	/**
	 * @see org.olat.core.util.vfs.VFSContainer#createChildContainer(java.lang.String)
	 */
	public VFSContainer createChildContainer(String name) {
		File fNewFile = new File(getBasefile(), name);
		if (!fNewFile.mkdir()) return null;
		LocalFolderImpl locFI =  new LocalFolderImpl(fNewFile, this);
		locFI.setDefaultItemFilter(defaultFilter);
		return locFI;
	}

	/**
	 * @see org.olat.core.util.vfs.VFSContainer#createChildLeaf(java.lang.String)
	 */
	public VFSLeaf createChildLeaf(String name) {
		File fNewFile = new File(getBasefile(), name);
		try {
			if(!fNewFile.getParentFile().exists()) {
				fNewFile.getParentFile().mkdirs();
			}
			if (!fNewFile.createNewFile()) {
				log.warn("Could not create a new leaf::" + name + " in container::" + getBasefile().getAbsolutePath() + " - file alreay exists");
				return null;
			} 
		} catch (Exception e) {
			log.error("Error while creating child leaf::" + name + " in container::" + getBasefile().getAbsolutePath(), e);
			return null;
		}
		return new LocalFileImpl(fNewFile, this);
	}

	/**
	 * @see org.olat.core.util.vfs.VFSContainer#getDefaultItemFilter()
	 */
	public VFSItemFilter getDefaultItemFilter() {
		return defaultFilter;
	}
	
}

