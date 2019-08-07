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

package org.olat.ims.cp;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.dom4j.tree.DefaultDocument;
import org.dom4j.tree.DefaultElement;
import org.olat.core.gui.control.generic.iframe.DeliveryOptions;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.FileUtils;
import org.olat.core.util.ZipUtil;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSManager;
import org.olat.core.util.xml.XMLParser;
import org.olat.core.util.xml.XStreamHelper;
import org.olat.fileresource.FileResourceManager;
import org.olat.ims.cp.objects.CPOrganization;
import org.olat.ims.cp.objects.CPResource;
import org.olat.ims.cp.ui.CPPackageConfig;
import org.olat.ims.cp.ui.CPPage;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;

import com.thoughtworks.xstream.XStream;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContext;

/**
 * The CP manager implementation.
 * <p>
 * In many cases, method calls are delegated to the content package object.
 * 
 * <P>
 * Initial Date: 04.07.2008 <br>
 * 
 * @author Sergio Trentini
 */
public class CPManagerImpl extends CPManager {
	
	private static final OLog log = Tracing.createLoggerFor(CPManagerImpl.class);

	public static final String PACKAGE_CONFIG_FILE_NAME = "CPPackageConfig.xml";

	private static XStream configXstream = XStreamHelper.createXStreamInstance();
	static {
		configXstream.alias("packageConfig", CPPackageConfig.class);
		configXstream.alias("deliveryOptions", DeliveryOptions.class);
	}

	private final ServletContext servletContext;
	
	/**
	 * [spring]
	 */
	@Autowired
	private CPManagerImpl(ServletContext servletContext) {
		this.servletContext = servletContext;
		INSTANCE = this;
	}

	@Override
	public CPPackageConfig getCPPackageConfig(OLATResourceable ores) {
		FileResourceManager frm = FileResourceManager.getInstance();
		File reFolder = frm.getFileResourceRoot(ores);
		File configXml = new File(reFolder, PACKAGE_CONFIG_FILE_NAME);
		
		CPPackageConfig config;
		if(configXml.exists()) {
			config = (CPPackageConfig)configXstream.fromXML(configXml);
		} else {
			//set default config
			config = new CPPackageConfig();
			config.setDeliveryOptions(DeliveryOptions.defaultWithGlossary());
			setCPPackageConfig(ores, config);
		}
		return config;
	}

	@Override
	public void setCPPackageConfig(OLATResourceable ores, CPPackageConfig config) {
		FileResourceManager frm = FileResourceManager.getInstance();
		File reFolder = frm.getFileResourceRoot(ores);
		File configXml = new File(reFolder, PACKAGE_CONFIG_FILE_NAME);
		if(config == null) {
			if(configXml.exists()) {
				configXml.delete();
			}
		} else {
			OutputStream out = null;
			try {
				out = new FileOutputStream(configXml);
				configXstream.toXML(config, out);
			} catch (IOException e) {
				log.error("", e);
			} finally {
				IOUtils.closeQuietly(out);
			}
		}
	}

	/**
	 * 
	 * @see org.olat.ims.cp.CPManager#load(org.olat.core.util.vfs.VFSContainer)
	 */
	public ContentPackage load(VFSContainer directory, OLATResourceable ores) {
		XMLParser parser = new XMLParser();
		ContentPackage cp;

		VFSLeaf file = (VFSLeaf) directory.resolve("imsmanifest.xml");

		if (file != null) {
			try {
				DefaultDocument doc = (DefaultDocument) parser.parse(file.getInputStream(), false);
				cp = new ContentPackage(doc, directory, ores);
				// If a wiki is imported or a new cp created, set a unique orga
				// identifier.
				if (cp.getLastError() == null) {
					if (cp.isOLATContentPackage() && CPCore.OLAT_ORGANIZATION_IDENTIFIER.equals(cp.getFirstOrganizationInManifest().getIdentifier())) {
						setUniqueOrgaIdentifier(cp);
					}
				}

			} catch (OLATRuntimeException e) {
				cp = new ContentPackage(null, directory, ores);
				logError("Reading imsmanifest failed. Dir: " + directory.getName() + ". Ores: " + ores.getResourceableId(), e);
				cp.setLastError("Exception reading XML for IMS CP: invalid xml-file ( " + directory.getName() + ")");
			}

		} else {
			cp = new ContentPackage(null, directory, ores);
			cp.setLastError("Exception reading XML for IMS CP: IMS-Manifest not found in " + directory.getName());
			logError("IMS manifiest xml couldn't be found in dir " + directory.getName() + ". Ores: " + ores.getResourceableId(), null);
			throw new OLATRuntimeException(CPManagerImpl.class, "The imsmanifest.xml file was not found.", new IOException());
		}
		return cp;
	}

	/**
	 * @see org.olat.ims.cp.CPManager#createNewCP(org.olat.core.id.OLATResourceable)
	 */
	public ContentPackage createNewCP(OLATResourceable ores, String initalPageTitle) {
		// copy template cp to new repo-location
		if (copyTemplCP(ores)) {
			File cpRoot = FileResourceManager.getInstance().unzipFileResource(ores);
			logDebug("createNewCP: cpRoot=" + cpRoot);
			logDebug("createNewCP: cpRoot.getAbsolutePath()=" + cpRoot.getAbsolutePath());
			LocalFolderImpl vfsWrapper = new LocalFolderImpl(cpRoot);
			ContentPackage cp = load(vfsWrapper, ores);

			// Modify the copy of the template to get a unique identifier
			CPOrganization orga = setUniqueOrgaIdentifier(cp);
			setOrgaTitleToRepoEntryTitle(ores, orga);
			// Also set the translated title of the inital page.
			orga.getItems().get(0).setTitle(initalPageTitle);

			writeToFile(cp);
			
			//set the default settings for file delivery
			DeliveryOptions defOptions = DeliveryOptions.defaultWithGlossary();
			CPPackageConfig config = new CPPackageConfig();
			config.setDeliveryOptions(defOptions);
			setCPPackageConfig(ores, config);

			return cp;

		} else {
			logError("CP couldn't be created. Error when copying template. Ores: " + ores.getResourceableId(), null);
			throw new OLATRuntimeException("ERROR while creating new empty cp. an error occured while trying to copy template CP", null);
		}
	}

	/**
	 * Sets the organization title to the title of the repository entry.
	 * 
	 * @param ores
	 * @param orga
	 */
	private void setOrgaTitleToRepoEntryTitle(OLATResourceable ores, CPOrganization orga) {
		// Set the title of the organization to the title of the resource.
		RepositoryManager resMgr = RepositoryManager.getInstance();
		RepositoryEntry cpEntry = resMgr.lookupRepositoryEntry(ores, false);
		if (cpEntry != null) {
			String title = cpEntry.getDisplayname();
			orga.setTitle(title);
		}
	}

	/**
	 * Assigns the organization a unique identifier in order to prevent any
	 * caching issues in the extjs menu tree later.
	 * 
	 * @param cp
	 * @return The first organization of the content package.
	 */
	private CPOrganization setUniqueOrgaIdentifier(ContentPackage cp) {
		CPOrganization orga = cp.getFirstOrganizationInManifest();
		String newOrgaIdentifier = "olatcp-" + CodeHelper.getForeverUniqueID();
		orga.setIdentifier(newOrgaIdentifier);
		return orga;
	}

	public boolean isSingleUsedResource(CPResource res, ContentPackage cp) {
		return cp.isSingleUsedResource(res);
	}

	@Override
	public String addBlankPage(ContentPackage cp, String title) {
		return cp.addBlankPage(title);
	}

	@Override
	public String addBlankPage(ContentPackage cp, String title, String parentNodeID) {
		return cp.addBlankPage(parentNodeID, title);
	}

	@Override
	public void updatePage(ContentPackage cp, CPPage page) {
		cp.updatePage(page);
	}

	/**
	 * @see org.olat.ims.cp.CPManager#addElement(org.olat.ims.cp.ContentPackage,
	 *      org.dom4j.tree.DefaultElement)
	 */
	public boolean addElement(ContentPackage cp, DefaultElement newElement) {
		return cp.addElement(newElement);

	}

	/**
	 * @see org.olat.ims.cp.CPManager#addElement(org.olat.ims.cp.ContentPackage,
	 *      org.dom4j.tree.DefaultElement, java.lang.String)
	 */
	public boolean addElement(ContentPackage cp, DefaultElement newElement, String parentIdentifier, int position) {
		return cp.addElement(newElement, parentIdentifier, position);
	}

	/**
	 * 
	 * @see org.olat.ims.cp.CPManager#addElementAfter(org.olat.ims.cp.ContentPackage,
	 *      org.dom4j.tree.DefaultElement, java.lang.String)
	 */
	public boolean addElementAfter(ContentPackage cp, DefaultElement newElement, String identifier) {
		return cp.addElementAfter(newElement, identifier);
	}

	/**
	 * 
	 * @see org.olat.ims.cp.CPManager#removeElement(org.olat.ims.cp.ContentPackage,
	 *      java.lang.String)
	 */
	public void removeElement(ContentPackage cp, String identifier, boolean deleteResource) {
		cp.removeElement(identifier, deleteResource);
	}

	/**
	 * @see org.olat.ims.cp.CPManager#moveElement(org.olat.ims.cp.ContentPackage,
	 *      java.lang.String, java.lang.String, int)
	 */
	public void moveElement(ContentPackage cp, String nodeID, String newParentID, int position) {
		cp.moveElement(nodeID, newParentID, position);
	}

	/**
	 * 
	 * @see org.olat.ims.cp.CPManager#copyElement(org.olat.ims.cp.ContentPackage,
	 *      java.lang.String)
	 */
	public String copyElement(ContentPackage cp, String sourceID) {
		return cp.copyElement(sourceID, sourceID);
	}

	/**
	 * @see org.olat.ims.cp.CPManager#getDocument(org.olat.ims.cp.ContentPackage)
	 */
	public DefaultDocument getDocument(ContentPackage cp) {
		return cp.getDocument();
	}

	public String getItemTitle(ContentPackage cp, String itemID) {
		return cp.getItemTitle(itemID);
	}

	/**
	 * @see org.olat.ims.cp.CPManager#getElementByIdentifier(org.olat.ims.cp.ContentPackage,
	 *      java.lang.String)
	 */
	public DefaultElement getElementByIdentifier(ContentPackage cp, String identifier) {
		return cp.getElementByIdentifier(identifier);
	}

	@Override
	public CPTreeDataModel getTreeDataModel(ContentPackage cp) {
		return cp.buildTreeDataModel();
	}

	/**
	 * 
	 * @see org.olat.ims.cp.CPManager#getFirstOrganizationInManifest(org.olat.ims.cp.ContentPackage)
	 */
	@Override
	public CPOrganization getFirstOrganizationInManifest(ContentPackage cp) {
		return cp.getFirstOrganizationInManifest();
	}

	/**
	 * 
	 * @see org.olat.ims.cp.CPManager#getFirstPageToDisplay(org.olat.ims.cp.ContentPackage)
	 */
	@Override
	public CPPage getFirstPageToDisplay(ContentPackage cp) {
		return cp.getFirstPageToDisplay();
	}

	/**
	 * @see org.olat.ims.cp.CPManager#WriteToFile(org.olat.ims.cp.ContentPackage)
	 */
	public void writeToFile(ContentPackage cp) {
		cp.writeToFile();
	}

	/**
	 * @see org.olat.ims.cp.CPManager#writeToZip(org.olat.ims.cp.ContentPackage)
	 */
	public VFSLeaf writeToZip(ContentPackage cp) {
		OLATResourceable ores = cp.getResourcable();
		VFSContainer cpRoot = cp.getRootDir();
		VFSContainer oresRoot = FileResourceManager.getInstance().getFileResourceRootImpl(ores);
		RepositoryEntry repoEntry = RepositoryManager.getInstance().lookupRepositoryEntry(ores, false);
		String zipFileName = "imscp.zip";
		if (repoEntry != null) {
			String zipName = repoEntry.getResourcename();
			if (zipName != null && zipName.endsWith(".zip")) {
				zipFileName = zipName;
			}
		}
		// delete old archive and create new one
		VFSItem oldArchive = oresRoot.resolve(zipFileName);
		if (oldArchive != null) {
			oldArchive.deleteSilently();//don't versioned the zip
		}
		ZipUtil.zip(cpRoot.getItems(), oresRoot.createChildLeaf(zipFileName), true);
		VFSLeaf zip = (VFSLeaf) oresRoot.resolve(zipFileName);
		return zip;
	}

	/**
	 * 
	 * @see org.olat.ims.cp.CPManager#getPageByItemId(org.olat.ims.cp.ContentPackage,
	 *      java.lang.String)
	 */
	@Override
	public String getPageByItemId(ContentPackage cp, String itemIdentifier) {
		return cp.getPageByItemId(itemIdentifier);
	}

	/**
	 * copies the default,empty, cp template to the new ores-directory
	 * 
	 * @param ores
	 * @return
	 */
	private boolean copyTemplCP(OLATResourceable ores) {
		File root = FileResourceManager.getInstance().getFileResourceRoot(ores);

		String packageName = ContentPackage.class.getCanonicalName();
		String path = packageName.replace('.', '/');
		path = path.replace("/ContentPackage", "/_resources/imscp.zip");

		path = VFSManager.sanitizePath(path);
		try {
			URL url = servletContext.getResource(path);
			assert url != null;
			try {
				InputStream inputStream = url.openConnection().getInputStream();
				FileUtils.saveToDir(inputStream, root, "imscp.zip");
				return true;
			} catch (IOException e) {
				logError("cp template was not copied. Source:  " + url + " Target: " + root.getAbsolutePath(), null);
			}
		} catch (MalformedURLException e) {
			logError("Bad url syntax when copying cp template. url: " + path + " Ores:" + ores.getResourceableId(), null);
		}

		return false;
	}

}
