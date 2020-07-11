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
import org.olat.test.OlatTestCase;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Locale;


public class VideoDisplayControllerTest extends OlatTestCase {

    HttpServer httpServer;
    int port;

    public static int generateRandomPort() {
        ServerSocket s = null;
        try {
            // ServerSocket(0) results in availability of a free random port
            s = new ServerSocket(0);
            return s.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            assert s != null;
            try {
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Before
    public void setUp() throws IOException {
        port = generateRandomPort();
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/embed/78ece87d", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                byte[] response = "<h1>Embed SWITCHtube videos</h1>".getBytes();
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            }
        });
        httpServer.start();
    }

    @After
    public void tearDown() {
        httpServer.stop(0);
    }

    @Test
    public void getSwitchTubeSource() throws IOException {
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
        videoManager.createVideoMetadata(videoEntry,  "http://localhost:" + port + "/videos/78ece87d", VideoFormat.switchtube);

        RepositoryService repositoryService = CoreSpringFactory.getImpl(RepositoryService.class);
        repositoryService.update(videoEntry);

        VideoDisplayController vdc = new VideoDisplayController(ureq, new WindowControlMocker(), videoEntry);

        // The source is already fetched in the constructor
        Assert.assertTrue(vdc.getSwitchTubeSource(false).contains("<h1>Embed SWITCHtube videos</h1>"));
    }
}