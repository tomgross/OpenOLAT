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
*/

package org.olat.repository.ui.author;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.olat.NewControllerFactory;
import org.olat.core.commons.services.license.LicenseModule;
import org.olat.core.commons.services.license.LicenseService;
import org.olat.core.commons.services.license.LicenseType;
import org.olat.core.commons.services.license.ResourceLicense;
import org.olat.core.commons.services.license.ui.LicenseSelectionConfig;
import org.olat.core.commons.services.license.ui.LicenseUIFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.DateChooser;
import org.olat.core.gui.components.form.flexible.elements.FileElement;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.TextAreaElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.FileElementEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.FormSubmit;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.FileUtils;
import org.olat.core.util.StringHelper;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.event.MultiUserEvent;
import org.olat.core.util.vfs.LocalFileImpl;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.course.CourseModule;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryManagedFlag;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryService;
import org.olat.repository.controllers.EntryChangedEvent;
import org.olat.repository.controllers.EntryChangedEvent.Change;
import org.olat.repository.handlers.RepositoryHandler;
import org.olat.repository.handlers.RepositoryHandlerFactory;
import org.olat.repository.manager.RepositoryEntryLicenseHandler;
import org.olat.repository.manager.RepositoryEntryLifecycleDAO;
import org.olat.repository.model.RepositoryEntryLifecycle;
import org.olat.resource.OLATResource;
import org.olat.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Description:<br>
 * 
 * @author Ingmar Kroll
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 * 
 */
public class RepositoryEditDescriptionController extends FormBasicController {
	
	private static final Set<String> imageMimeTypes = new HashSet<>();
	static {
		imageMimeTypes.add("image/gif");
		imageMimeTypes.add("image/jpg");
		imageMimeTypes.add("image/jpeg");
		imageMimeTypes.add("image/png");
	}

	private VFSContainer mediaContainer;
	private RepositoryEntry repositoryEntry;
	private final String repoEntryType;
	private ResourceLicense license;

	private static final int picUploadlimitKB = 5120;
	private static final int movieUploadlimitKB = 102400;

	private FileElement fileUpload, movieUpload;
	private TextElement externalRef, displayName, authors, expenditureOfWork, language, location, licensorEl;
	private TextAreaElement licenseFreetextEl;
	private RichTextElement description, objectives, requirements, credits;
	private SingleSelection dateTypesEl, publicDatesEl, licenseEl;
	private DateChooser startDateEl, endDateEl;
	private FormSubmit submit;
	private FormLayoutContainer privateDatesCont;
	
	private static final String[] dateKeys = new String[]{ "none", "private", "public"};

	@Autowired
	private UserManager userManager;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private RepositoryManager repositoryManager;
	@Autowired
	private RepositoryEntryLifecycleDAO lifecycleDao;
	@Autowired
	private RepositoryHandlerFactory repositoryHandlerFactory;
	@Autowired
	private LicenseService licenseService;
	@Autowired
	private LicenseModule licenseModule;
	@Autowired
	private RepositoryEntryLicenseHandler licenseHandler;

	/**
	 * Create a repository add controller that adds the given resourceable.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param sourceEntry
	 */
	public RepositoryEditDescriptionController(UserRequest ureq, WindowControl wControl, RepositoryEntry entry) {
		super(ureq, wControl);
		setBasePackage(RepositoryService.class);
		this.repositoryEntry = entry;
		repoEntryType = repositoryEntry.getOlatResource().getResourceableTypeName();
		initForm(ureq);
	}
	
	public RepositoryEntry getEntry() {
		return repositoryEntry;
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormContextHelp("Set up info page");
		formLayout.setElementCssClass("o_sel_edit_repositoryentry");

		String id = repositoryEntry.getResourceableId() == null ? "-" : repositoryEntry.getResourceableId().toString();
		uifactory.addStaticTextElement("cif.id", id, formLayout);
		
		String externalId = repositoryEntry.getExternalId();
		if(StringHelper.containsNonWhitespace(externalId)) {
			uifactory.addStaticTextElement("cif.externalid", externalId, formLayout);
		}
		
		String extRef = repositoryEntry.getExternalRef();
		if(StringHelper.containsNonWhitespace(repositoryEntry.getManagedFlagsString())) {
			if(StringHelper.containsNonWhitespace(extRef)) {
				uifactory.addStaticTextElement("cif.externalref", extRef, formLayout);
			}
		} else {
			externalRef = uifactory.addTextElement("cif.externalref", "cif.externalref", 100, extRef, formLayout);
			externalRef.setHelpText(translate("cif.externalref.hover"));
			externalRef.setHelpUrlForManualPage("Info Page#_identification");
			externalRef.setDisplaySize(30);
		}

		String initalAuthor = repositoryEntry.getInitialAuthor() == null ? "-" : repositoryEntry.getInitialAuthor();
		if(repositoryEntry.getInitialAuthor() != null) {
			initalAuthor = userManager.getUserDisplayName(initalAuthor);
		}
		initalAuthor = StringHelper.escapeHtml(initalAuthor);
		uifactory.addStaticTextElement("cif.initialAuthor", initalAuthor, formLayout);
		// Add resource type
		String typeName = null;
		OLATResource res = repositoryEntry.getOlatResource();
		if (res != null) {
			typeName = res.getResourceableTypeName();
		}
		
		String typeDisplay ;
		if (typeName != null) { // add image and typename code
			typeDisplay = NewControllerFactory.translateResourceableTypeName(typeName, getLocale());
		} else {
			typeDisplay = translate("cif.type.na");
		}
		uifactory.addStaticTextElement("cif.type", typeDisplay, formLayout);
		
		uifactory.addSpacerElement("spacer1", formLayout, false);

		displayName = uifactory.addTextElement("cif.displayname", "cif.displayname", 100, repositoryEntry.getDisplayname(), formLayout);
		displayName.setDisplaySize(30);
		displayName.setMandatory(true);
		displayName.setEnabled(!RepositoryEntryManagedFlag.isManaged(repositoryEntry, RepositoryEntryManagedFlag.title));

		authors = uifactory.addTextElement("cif.authors", "cif.authors", 255, repositoryEntry.getAuthors(), formLayout);
		authors.setDisplaySize(60);
		
		if (licenseModule.isEnabled(licenseHandler)) {
			license = licenseService.loadOrCreateLicense(res);

			LicenseSelectionConfig licenseSelectionConfig = LicenseUIFactory
					.createLicenseSelectionConfig(licenseHandler, license.getLicenseType());
			licenseEl = uifactory.addDropdownSingleselect("cif.license", formLayout,
					licenseSelectionConfig.getLicenseTypeKeys(),
					licenseSelectionConfig.getLicenseTypeValues(getLocale()));
			licenseEl.setElementCssClass("o_sel_repo_license");
			licenseEl.setMandatory(licenseSelectionConfig.isLicenseMandatory());
			if (licenseSelectionConfig.getSelectionLicenseTypeKey() != null) {
				licenseEl.select(licenseSelectionConfig.getSelectionLicenseTypeKey(), true);
			}
			licenseEl.addActionListener(FormEvent.ONCHANGE);
			
			licensorEl = uifactory.addTextElement("cif.licensor", 1000, license.getLicensor(), formLayout);

			String freetext = licenseService.isFreetext(license.getLicenseType()) ? license.getFreetext() : "";
			licenseFreetextEl = uifactory.addTextAreaElement("cif.freetext", 4, 72, freetext, formLayout);
			LicenseUIFactory.updateVisibility(licenseEl, licensorEl, licenseFreetextEl);
		}
		
		language = uifactory.addTextElement("cif.mainLanguage", "cif.mainLanguage", 16, repositoryEntry.getMainLanguage(), formLayout);
		
		location = uifactory.addTextElement("cif.location", "cif.location", 255, repositoryEntry.getLocation(), formLayout);
		location.setEnabled(!RepositoryEntryManagedFlag.isManaged(repositoryEntry, RepositoryEntryManagedFlag.location));
		
		RepositoryHandler handler = repositoryHandlerFactory.getRepositoryHandler(repositoryEntry);
		mediaContainer = handler.getMediaContainer(repositoryEntry);
		if(mediaContainer != null && mediaContainer.getName().equals("media")) {
			mediaContainer = mediaContainer.getParentContainer();
			mediaContainer.setDefaultItemFilter(new MediaContainerFilter(mediaContainer));
		}
		
		String desc = (repositoryEntry.getDescription() != null ? repositoryEntry.getDescription() : " ");
		description = uifactory.addRichTextElementForStringData("cif.description", "cif.description",
				desc, 10, -1, false, mediaContainer, null, formLayout, ureq.getUserSession(), getWindowControl());
		description.setEnabled(!RepositoryEntryManagedFlag.isManaged(repositoryEntry, RepositoryEntryManagedFlag.description));
		description.getEditorConfiguration().setFileBrowserUploadRelPath("media");

		uifactory.addSpacerElement("spacer2", formLayout, false);

		if(CourseModule.getCourseTypeName().equals(repoEntryType)) {
			String[] dateValues = new String[] {
					translate("cif.dates.none"),
					translate("cif.dates.private"),
					translate("cif.dates.public")	
			};
			dateTypesEl = uifactory.addRadiosVertical("cif.dates", formLayout, dateKeys, dateValues);
			dateTypesEl.setElementCssClass("o_sel_repo_lifecycle_type");
			if(repositoryEntry.getLifecycle() == null) {
				dateTypesEl.select("none", true);
			} else if(repositoryEntry.getLifecycle().isPrivateCycle()) {
				dateTypesEl.select("private", true);
			} else {
				dateTypesEl.select("public", true);
			}
			dateTypesEl.addActionListener(FormEvent.ONCHANGE);
	
			List<RepositoryEntryLifecycle> cycles = lifecycleDao.loadPublicLifecycle();
			List<RepositoryEntryLifecycle> filteredCycles = new ArrayList<>();
			//just make the upcomming and acutual running cycles or the pre-selected visible in the UI
			LocalDateTime now = LocalDateTime.now();
			for(RepositoryEntryLifecycle cycle:cycles) {
				if(cycle.getValidTo() == null
						|| now.isBefore(LocalDateTime.ofInstant(cycle.getValidTo().toInstant(), ZoneId.systemDefault()))
						|| (repositoryEntry.getLifecycle() != null && repositoryEntry.getLifecycle().equals(cycle))) {
					filteredCycles.add(cycle);
				}
			}
			
			String[] publicKeys = new String[filteredCycles.size()];
			String[] publicValues = new String[filteredCycles.size()];
			int count = 0;		
			for(RepositoryEntryLifecycle cycle:filteredCycles) {
					publicKeys[count] = cycle.getKey().toString();
					
					StringBuilder sb = new StringBuilder(32);
					boolean labelAvailable = StringHelper.containsNonWhitespace(cycle.getLabel());
					if(labelAvailable) {
						sb.append(cycle.getLabel());
					}
					if(StringHelper.containsNonWhitespace(cycle.getSoftKey())) {
						if(labelAvailable) sb.append(" - ");
						sb.append(cycle.getSoftKey());
					}
					publicValues[count++] = sb.toString();
			}
			publicDatesEl = uifactory.addDropdownSingleselect("cif.public.dates", formLayout, publicKeys, publicValues, null);
	
			String privateDatePage = velocity_root + "/cycle_dates.html";
			privateDatesCont = FormLayoutContainer.createCustomFormLayout("private.date", getTranslator(), privateDatePage);
			privateDatesCont.setRootForm(mainForm);
			privateDatesCont.setLabel("cif.private.dates", null);
			formLayout.add("private.date", privateDatesCont);
			
			startDateEl = uifactory.addDateChooser("date.start", "cif.date.start", null, privateDatesCont);
			startDateEl.setElementCssClass("o_sel_repo_lifecycle_validfrom");
			endDateEl = uifactory.addDateChooser("date.end", "cif.date.end", null, privateDatesCont);
			endDateEl.setElementCssClass("o_sel_repo_lifecycle_validto");
			
			if(repositoryEntry.getLifecycle() != null) {
				RepositoryEntryLifecycle lifecycle = repositoryEntry.getLifecycle();
				if(lifecycle.isPrivateCycle()) {
					startDateEl.setDate(lifecycle.getValidFrom());
					endDateEl.setDate(lifecycle.getValidTo());
				} else {
					String key = lifecycle.getKey().toString();
					for(String publicKey:publicKeys) {
						if(key.equals(publicKey)) {
							publicDatesEl.select(key, true);
							break;
						}
					}
				}
			}
	
			updateDatesVisibility();
			uifactory.addSpacerElement("spacer3", formLayout, false);
			
			expenditureOfWork = uifactory.addTextElement("cif.expenditureOfWork", "cif.expenditureOfWork", 100, repositoryEntry.getExpenditureOfWork(), formLayout);
			expenditureOfWork.setExampleKey("details.expenditureOfWork.example", null);

			String obj = (repositoryEntry.getObjectives() != null ? repositoryEntry.getObjectives() : " ");
			objectives = uifactory.addRichTextElementForStringData("cif.objectives", "cif.objectives",
					obj, 10, -1, false, mediaContainer, null, formLayout, ureq.getUserSession(), getWindowControl());
			objectives.setEnabled(!RepositoryEntryManagedFlag.isManaged(repositoryEntry, RepositoryEntryManagedFlag.objectives));
			objectives.getEditorConfiguration().setFileBrowserUploadRelPath("media");
			
			
			String req = (repositoryEntry.getRequirements() != null ? repositoryEntry.getRequirements() : " ");
			requirements = uifactory.addRichTextElementForStringData("cif.requirements", "cif.requirements",
					req, 10, -1,  false, mediaContainer, null, formLayout, ureq.getUserSession(), getWindowControl());
			requirements.setEnabled(!RepositoryEntryManagedFlag.isManaged(repositoryEntry, RepositoryEntryManagedFlag.requirements));
			requirements.getEditorConfiguration().setFileBrowserUploadRelPath("media");
			requirements.setMaxLength(2000);
			
			String cred = (repositoryEntry.getCredits() != null ? repositoryEntry.getCredits() : " ");
			credits = uifactory.addRichTextElementForStringData("cif.credits", "cif.credits",
					cred, 10, -1,  false, mediaContainer, null, formLayout, ureq.getUserSession(), getWindowControl());
			credits.setEnabled(!RepositoryEntryManagedFlag.isManaged(repositoryEntry, RepositoryEntryManagedFlag.credits));
			credits.getEditorConfiguration().setFileBrowserUploadRelPath("media");
			credits.setMaxLength(2000);
			
			uifactory.addSpacerElement("spacer4", formLayout, false);
		}
		
		boolean managed = RepositoryEntryManagedFlag.isManaged(repositoryEntry, RepositoryEntryManagedFlag.details);
		
		VFSLeaf img = repositoryManager.getImage(repositoryEntry);
		fileUpload = uifactory.addFileElement(getWindowControl(), "rentry.pic", "rentry.pic", formLayout);
		fileUpload.setExampleKey("rentry.pic.example", new String[] {RepositoryManager.PICTURE_WIDTH + "x" + (RepositoryManager.PICTURE_HEIGHT)});
		fileUpload.setMaxUploadSizeKB(picUploadlimitKB, null, null);
		fileUpload.setPreview(ureq.getUserSession(), true);
		fileUpload.addActionListener(FormEvent.ONCHANGE);
		fileUpload.setDeleteEnabled(!managed);
		if(img instanceof LocalFileImpl) {
			fileUpload.setPreview(ureq.getUserSession(), true);
			fileUpload.setInitialFile(((LocalFileImpl)img).getBasefile());
		}
		fileUpload.setVisible(!managed);
		fileUpload.limitToMimeType(imageMimeTypes, "cif.error.mimetype", new String[]{ imageMimeTypes.toString()} );

		VFSLeaf movie = repositoryService.getIntroductionMovie(repositoryEntry);
		movieUpload = uifactory.addFileElement(getWindowControl(), "rentry.movie", "rentry.movie", formLayout);
		movieUpload.setExampleKey("rentry.movie.example", new String[] {"3:2"});
		movieUpload.setMaxUploadSizeKB(movieUploadlimitKB, null, null);
		movieUpload.setPreview(ureq.getUserSession(), true);
		movieUpload.addActionListener(FormEvent.ONCHANGE);
		movieUpload.setDeleteEnabled(!managed);
		if(movie instanceof LocalFileImpl) {
			movieUpload.setPreview(ureq.getUserSession(), true);
			movieUpload.setInitialFile(((LocalFileImpl)movie).getBasefile());
		}
		movieUpload.setVisible(!managed);

		FormLayoutContainer buttonContainer = FormLayoutContainer.createButtonLayout("buttonContainer", getTranslator());
		formLayout.add("buttonContainer", buttonContainer);
		buttonContainer.setElementCssClass("o_sel_repo_save_details");
		submit = uifactory.addFormSubmitButton("submit", buttonContainer);
		submit.setVisible(!managed);
		uifactory.addFormCancelButton("cancel", buttonContainer, ureq, getWindowControl());
	}

	private void updateDatesVisibility() {
		if(dateTypesEl.isOneSelected()) {
			String type = dateTypesEl.getSelectedKey();
			if("none".equals(type)) {
				publicDatesEl.setVisible(false);
				privateDatesCont.setVisible(false);
			} else if("public".equals(type)) {
				publicDatesEl.setVisible(true);
				privateDatesCont.setVisible(false);
			} else if("private".equals(type)) {
				publicDatesEl.setVisible(false);
				privateDatesCont.setVisible(true);
			}
		}
	}

	@Override
	protected void doDispose() {
		// Controllers autodisposed by basic controller
	}

	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOk = super.validateFormLogic(ureq);
		
		// Check for empty display name
		if (!StringHelper.containsNonWhitespace(displayName.getValue())) {
			displayName.setErrorKey("cif.error.displayname.empty", new String[] {});
			allOk = false;
		} else if (displayName.hasError()) {
			allOk = false;
		} else {
			displayName.clearError();
		}

		allOk &= validateTextElement(language, 255);
		allOk &= validateTextElement(location, 255);
		allOk &= validateTextElement(objectives, 2000);
		allOk &= validateTextElement(requirements, 2000);
		allOk &= validateTextElement(credits, 2000);
		allOk &= validateTextElement(externalRef, 58);
		allOk &= validateTextElement(expenditureOfWork, 225);
		allOk &= validateTextElement(authors, 2000);
		
		if (publicDatesEl != null) {
			publicDatesEl.clearError();
			if(publicDatesEl.isEnabled() && publicDatesEl.isVisible()) {
				if(!publicDatesEl.isOneSelected()) {
					publicDatesEl.setErrorKey("form.legende.mandatory", null);
					allOk &= false;
				}	
			}
		}
		
		if (licenseEl != null) {
			licenseEl.clearError();
			if (LicenseUIFactory.validateLicenseTypeMandatoryButNonSelected(licenseEl)) {
				licenseEl.setErrorKey("form.legende.mandatory", null);
				allOk &= false;
			}
		}

		return allOk;
	}
	
	private boolean validateTextElement(TextElement el, int maxLength) {
		boolean ok;
		if(el == null) {
			ok = true;
		} else {
			String val = el.getValue();
			el.clearError();
			if(val != null && val.length() > maxLength) {
				el.setErrorKey("input.toolong", new String[]{ Integer.toString(maxLength) });
				ok = false;
			} else {
				ok = true;
			}
		}
		return ok;
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if (source == dateTypesEl) {
			updateDatesVisibility();
		} else if (source == licenseEl) {
			LicenseUIFactory.updateVisibility(licenseEl, licensorEl, licenseFreetextEl);;
		} else if (source == fileUpload) {
			if(FileElementEvent.DELETE.equals(event.getCommand())) {
				fileUpload.clearError();
				VFSLeaf img = repositoryManager.getImage(repositoryEntry);
				if(fileUpload.getUploadFile() != null && fileUpload.getUploadFile() != fileUpload.getInitialFile()) {
					fileUpload.reset();
					if(img != null) {
						fileUpload.setInitialFile(((LocalFileImpl)img).getBasefile());
					}
				} else if(img != null) {
					repositoryManager.deleteImage(repositoryEntry);
					fileUpload.setInitialFile(null);
				}
				flc.setDirty(true);	
			}
		} else if (source == movieUpload) {
			if(FileElementEvent.DELETE.equals(event.getCommand())) {
				movieUpload.clearError();
				VFSLeaf movie = repositoryService.getIntroductionMovie(repositoryEntry);
				if(movieUpload.getUploadFile() != null && movieUpload.getUploadFile() != movieUpload.getInitialFile()) {
					movieUpload.reset();
					if(movie != null) {
						movieUpload.setInitialFile(((LocalFileImpl)movie).getBasefile());
					}
				} else if(movie != null) {
					movie.delete();
					movieUpload.setInitialFile(null);
				}
				flc.setDirty(true);
			}
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		if (licenseModule.isEnabled(licenseHandler)) {
			if (licenseEl != null && licenseEl.isOneSelected()) {
				String licenseTypeKey = licenseEl.getSelectedKey();
				LicenseType licneseType = licenseService.loadLicenseTypeByKey(licenseTypeKey);
				license.setLicenseType(licneseType);
			}
			String licensor = null;
			String freetext = null;
			if (licensorEl != null && licensorEl.isVisible()) {
				licensor = StringHelper.containsNonWhitespace(licensorEl.getValue())? licensorEl.getValue(): null;
			}
			if (licenseFreetextEl != null && licenseFreetextEl.isVisible()) {
				freetext = StringHelper.containsNonWhitespace(licenseFreetextEl.getValue())? licenseFreetextEl.getValue(): null;
			}
			license.setLicensor(licensor);
			license.setFreetext(freetext);
			license = licenseService.update(license);
			licensorEl.setValue(license.getLicensor());
			licenseFreetextEl.setValue(license.getFreetext());
		}
		
		File uploadedImage = fileUpload.getUploadFile();
		if(uploadedImage != null && uploadedImage.exists()) {
			VFSContainer tmpHome = new LocalFolderImpl(new File(WebappHelper.getTmpDir()));
			VFSContainer container = tmpHome.createChildContainer(UUID.randomUUID().toString());
			VFSLeaf newFile = fileUpload.moveUploadFileTo(container);//give it it's real name and extension
			boolean ok = repositoryManager.setImage(newFile, repositoryEntry);
			if (!ok) {
				showWarning("cif.error.image");
			} else {
				VFSLeaf image = repositoryManager.getImage(repositoryEntry);
				if(image instanceof  LocalFileImpl) {
					fileUpload.setInitialFile(((LocalFileImpl)image).getBasefile());
				}
			}
			container.delete();
		}

		File uploadedMovie = movieUpload.getUploadFile();
		if(uploadedMovie != null && uploadedMovie.exists()) {
			VFSContainer m = (VFSContainer)mediaContainer.resolve("media");
			VFSLeaf newFile = movieUpload.moveUploadFileTo(m);
			if (newFile == null) {
				showWarning("cif.error.movie");
			} else {
				String filename = movieUpload.getUploadFileName();
				String extension = FileUtils.getFileSuffix(filename);
				newFile.rename(repositoryEntry.getKey() + "." + extension);
			}
		}

		String displayname = displayName.getValue().trim();
		repositoryEntry.setDisplayname(displayname);
		
		String mainLanguage = language.getValue();
		if(StringHelper.containsNonWhitespace(mainLanguage)) {
			repositoryEntry.setMainLanguage(mainLanguage);
		} else {
			repositoryEntry.setMainLanguage(null);
		}
		
		if(dateTypesEl != null) {
			String type = "none";
			if(dateTypesEl.isOneSelected()) {
				type = dateTypesEl.getSelectedKey();
			}
			
			if("none".equals(type)) {
				repositoryEntry.setLifecycle(null);
			} else if("public".equals(type)) {
				String key = publicDatesEl.getSelectedKey();
				if(StringHelper.isLong(key)) {
					Long cycleKey = Long.parseLong(key);
					RepositoryEntryLifecycle cycle = lifecycleDao.loadById(cycleKey);
					repositoryEntry.setLifecycle(cycle);
				}
			} else if("private".equals(type)) {
				Date start = startDateEl.getDate();
				Date end = endDateEl.getDate();
				RepositoryEntryLifecycle cycle = repositoryEntry.getLifecycle();
				if(cycle == null || !cycle.isPrivateCycle()) {
					String softKey = "lf_" + repositoryEntry.getSoftkey();
					cycle = lifecycleDao.create(displayname, softKey, true, start, end);
				} else {
					cycle.setValidFrom(start);
					cycle.setValidTo(end);
					cycle = lifecycleDao.updateLifecycle(cycle);
				}
				repositoryEntry.setLifecycle(cycle);
			}
		}
		
		if(externalRef != null && externalRef.isEnabled()) {
			String ref = externalRef.getValue().trim();
			repositoryEntry.setExternalRef(ref);
		}
		
		String desc = description.getValue().trim();
		repositoryEntry.setDescription(desc);
		if(authors != null) {
			String auth = authors.getValue().trim();
			repositoryEntry.setAuthors(auth);
		}
		if(objectives != null) {
			String obj = objectives.getValue().trim();
			repositoryEntry.setObjectives(obj);
		}
		if(requirements != null) {
			String req = requirements.getValue().trim();
			repositoryEntry.setRequirements(req);
		}
		if(credits != null) {
			String cred = credits.getValue().trim();
			repositoryEntry.setCredits(cred);
		}
		if(expenditureOfWork != null) {
			String exp = expenditureOfWork.getValue().trim();
			repositoryEntry.setExpenditureOfWork(exp);
		}
		if(location != null) {
			String loc = location.getValue().trim();
			repositoryEntry.setLocation(loc);
		}
		
		repositoryEntry = repositoryManager.setDescriptionAndName(repositoryEntry,
				repositoryEntry.getDisplayname(), repositoryEntry.getExternalRef(), repositoryEntry.getAuthors(),
				repositoryEntry.getDescription(), repositoryEntry.getObjectives(), repositoryEntry.getRequirements(),
				repositoryEntry.getCredits(), repositoryEntry.getMainLanguage(), repositoryEntry.getLocation(),
				repositoryEntry.getExpenditureOfWork(), repositoryEntry.getLifecycle());
		if(repositoryEntry == null) {
			showWarning("repositoryentry.not.existing");
			fireEvent(ureq, Event.CLOSE_EVENT);
		} else {
			fireEvent(ureq, Event.CHANGED_EVENT);
			MultiUserEvent modifiedEvent = new EntryChangedEvent(repositoryEntry, getIdentity(), Change.modifiedDescription, "authoring");
			CoordinatorManager.getInstance().getCoordinator().getEventBus()
				.fireEventToListenersOf(modifiedEvent, RepositoryService.REPOSITORY_EVENT_ORES);
		}
	}

	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}

	/**
	 * @return Returns the repositoryEntry.
	 */
	public RepositoryEntry getRepositoryEntry() {
		return repositoryEntry;
	}
}