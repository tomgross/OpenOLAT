package org.olat.modules.video.ui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.olat.basesecurity.OrganisationService;
import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.DefaultGlobalSettings;
import org.olat.core.gui.GlobalSettings;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableComponent;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.panel.SimpleStackedPanel;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.render.RenderResult;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.gui.translator.Translator;
import org.olat.core.gui.util.SyntheticUserRequest;
import org.olat.core.gui.util.WindowControlMocker;
import org.olat.core.id.Identity;
import org.olat.core.id.Organisation;
import org.olat.core.id.Roles;
import org.olat.core.util.UserSession;
import org.olat.fileresource.types.VideoFileResource;
import org.olat.modules.video.VideoFormat;
import org.olat.modules.video.VideoManager;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryStatusEnum;
import org.olat.repository.RepositoryService;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;
import org.olat.test.JunitTestHelper;
import org.olat.test.KeyTranslator;
import org.olat.test.OlatTestCase;

import java.io.IOException;
import java.util.Locale;

import static org.olat.core.util.WebappHelper.getInstanceId;


public class VideoDisplayControllerTest extends OlatTestCase {

    @Test
    public void getSwitchTubeSource() {
        Identity id = JunitTestHelper.createAndPersistIdentityAsRndUser("archive-1");
        UserSession usess = new UserSession();
        usess.init();
        usess.setIdentity(id);
        usess.setRoles(Roles.userRoles());
        usess.reloadPreferences();
        SyntheticUserRequest ureq = new SyntheticUserRequest(id, Locale.ENGLISH, usess);

        OLATResource resource = OLATResourceManager.getInstance()
                .createOLATResourceInstance(new VideoFileResource());
        Organisation defOrganisation = CoreSpringFactory.getImpl(OrganisationService.class)
                .getDefaultOrganisation();
        RepositoryEntry videoEntry = CoreSpringFactory.getImpl(RepositoryService.class).create(
                id, "", "-", "Video - " + resource.getResourceableId(), "",
                resource, RepositoryEntryStatusEnum.preparation, defOrganisation);
        VideoManager videoManager = CoreSpringFactory.getImpl(VideoManager.class);
        videoManager.createVideoMetadata(videoEntry,  "https://tube.switch.ch/videos/78ece87d", VideoFormat.switchtube);

        RepositoryService repositoryService = CoreSpringFactory.getImpl(RepositoryService.class);
        repositoryService.update(videoEntry);

        WindowControl wControl = new WindowControlMocker();
        VideoDisplayController vdc = new VideoDisplayController(ureq, wControl, videoEntry);

        // The source is already fetched in the constructor
        //Assert.assertTrue(vdc.getSwitchTubeSource(false).contains("<h1>Embed SWITCHtube videos</h1>"));

        SimpleStackedPanel videopanel = (SimpleStackedPanel) vdc.getInitialComponent();
        StringOutput sb = new StringOutput();
        RenderResult renderResult = new RenderResult();
        String csrfToken = "28797b99-b63d-41ea-8750-2607a9fe2e52";
        URLBuilder ubu = new URLBuilder("/", getInstanceId(), "123", csrfToken);
        Translator translator = new KeyTranslator(Locale.ENGLISH);
        GlobalSettings gsettings = new DefaultGlobalSettings();
        // finally get the renderer and make sure the rendering of the table works
        Renderer renderer = Renderer.getInstance(videopanel, translator, ubu, renderResult, gsettings, csrfToken);

        videopanel.getContent().getHTMLRendererSingleton().render(
                renderer, sb, videopanel.getContent(), ubu, translator, renderResult, null);

        Assert.assertTrue(sb.toString().contains("<source type=\"video/mp4\" src=\"https://tube.switch.ch/external/78ece87d\""));
    }
}