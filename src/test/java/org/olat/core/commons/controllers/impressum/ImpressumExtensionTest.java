package org.olat.core.commons.controllers.impressum;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.olat.core.extensions.ExtensionElement;
import org.olat.core.gui.util.SyntheticUserRequest;
import org.olat.core.util.WebappHelper;
import org.olat.test.OlatTestCase;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;


public class ImpressumExtensionTest extends OlatTestCase {

    @Autowired
    ImpressumModule impressumModule;

    @Autowired
    ImpressumExtension impressumExtension;

    Path indexEnHtml;
    boolean currentImpressumEnabled;

    @Before
    public void setUp() throws Exception {
        currentImpressumEnabled = impressumModule.isEnabled();
        impressumModule.setEnabled(true);
        indexEnHtml = Paths.get(WebappHelper.getUserDataRoot(), "customizing", "impressum", "index_en.html");
        Path file = Files.createFile(indexEnHtml);
        String content = "<div>Hello Impressum</div>";
        Files.write( file, content.getBytes());
    }


    @After
    public void tearDown() throws Exception {
        Files.deleteIfExists(indexEnHtml);
        impressumModule.setEnabled(currentImpressumEnabled);
    }

    @Test
    public void getExtensionFor() {
        SyntheticUserRequest ureq = new SyntheticUserRequest(null, Locale.ENGLISH);
        ExtensionElement extElem = impressumExtension.getExtensionFor(
                "org.olat.core.commons.controllers.impressum.ImpressumMainController", ureq);
        assertThat(extElem).isInstanceOf(ImpressumExtension.class);
    }

    @Test
    public void getExtensionFor_NotEven_A_File() throws IOException {
        Files.deleteIfExists(indexEnHtml);
        indexEnHtml = Paths.get(WebappHelper.getUserDataRoot(), "customizing", "impressum", "index_en.html");
        indexEnHtml.toFile().mkdirs();

        SyntheticUserRequest ureq = new SyntheticUserRequest(null, Locale.ENGLISH);
        ExtensionElement extElem = impressumExtension.getExtensionFor(
                "org.olat.core.commons.controllers.impressum.ImpressumMainController", ureq);
        assertThat(extElem).isInstanceOf(ImpressumExtension.class);
    }
}