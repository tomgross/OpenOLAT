package org.olat.core.commons.controllers.impressum;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSManager;

import java.util.UUID;

import static org.junit.Assert.*;

public class GenericImpressumExtensionTest {

    GenericImpressumExtension genericImpressumExtension;

    @Before
    public void setUp() throws Exception {
        genericImpressumExtension = new GenericImpressumExtension(null, null);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void checkContent() {
        VFSContainer rootTest = VFSManager.olatRootContainer("/check-" + UUID.randomUUID(), null);
        String filename = UUID.randomUUID().toString() + ".html";
        VFSLeaf file = rootTest.createChildLeaf(filename);

    }
}