package org.olat.core.commons.controllers.impressum;

import org.apache.commons.io.FileUtils;
import org.olat.core.extensions.action.GenericActionExtension;
import org.olat.core.util.filter.FilterFactory;
import org.olat.core.util.i18n.I18nModule;
import org.olat.core.util.vfs.LocalFileImpl;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GenericImpressumExtension extends GenericActionExtension {

    protected final ImpressumModule impressumModule;
    protected final I18nModule i18nModule;

    public GenericImpressumExtension(ImpressumModule impressumModule, I18nModule i18nModule) {
        this.impressumModule = impressumModule;
        this.i18nModule = i18nModule;
    }

    boolean checkContent(VFSItem file) {
        boolean check = false;
        if(file instanceof VFSLeaf && file.exists() ) {
            if(file instanceof LocalFileImpl) {
                File f = ((LocalFileImpl)file).getBasefile();
                try {
                    String content = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
                    content = FilterFactory.getHtmlTagAndDescapingFilter().filter(content);
                    if(content.length() > 0) {
                        content = content.trim();
                    }
                    if(content.length() > 0) {
                        check = true;
                    }
                } catch (IOException e) {
                    // Nothing to to here
                }
            } else {
                check = true;
            }
        }
        return check;
    }

}
