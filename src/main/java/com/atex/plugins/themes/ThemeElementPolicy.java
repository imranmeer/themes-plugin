package com.atex.plugins.themes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.polopoly.cm.ContentId;
import com.polopoly.cm.app.policy.CheckboxPolicy;
import com.polopoly.cm.app.policy.ContentSingleSelectPolicy;
import com.polopoly.cm.app.policy.DuplicatorPolicy;
import com.polopoly.cm.app.policy.FilePolicy;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.policy.Policy;

/**
 * @author peterabjohns
 */
public class ThemeElementPolicy extends ElementPolicy {

    private static Logger LOG = Logger.getLogger(ThemeElementPolicy.class.getName());

    public List<WebFileResource> getFiles(String type) throws CMException, IOException {

        final String dupname = type + "dir";
        final String duppolicy = type + "file";

        final DuplicatorPolicy dup = (DuplicatorPolicy) getChildPolicy(dupname);

        if (dup != null) {
            final List duplist = dup.getDuplicatorElements();

            if (duplist != null) {
                final List<WebFileResource> files = new ArrayList<WebFileResource>(duplist.size());

                for (int i = 0; i < duplist.size(); i++) {

                    final FilePolicy fp = (FilePolicy) ((DuplicatorPolicy.DuplicatorElement) duplist.get(i)).getChildPolicy(duppolicy);
                    final String filePath = fp.getFilePath();
                    final String fileName = fp.getFileName();

                    if (filePath != null && fileName != null) {
                        final String fileFullName = fp.getFilePath() + "/" + fp.getFileName();
                        files.add(new WebFileResource(fp.getFileInfo(fileFullName)));
                    }
                }
                return files;
            }
        }
        return null;
    }

    public ThemeElementPolicy getBaseThemePolicy() {

        CheckboxPolicy isBasePol;
        try {

            isBasePol = (CheckboxPolicy) getChildPolicy("isBase");
            if (!isBasePol.getChecked()) {

                Policy basePol = getChildPolicy("base");
                ContentSingleSelectPolicy baseSelectPol = (ContentSingleSelectPolicy) basePol.getChildPolicy("baseElement");

                if (baseSelectPol.getReference() != null) {
                    ContentId baseContentId = baseSelectPol.getReference().getContentId();
                    return (ThemeElementPolicy) getCMServer().getPolicy(baseContentId);
                }
            }

        } catch (CMException e) {
            LOG.log(Level.WARNING, e.getLocalizedMessage(), e);
        }
        return null;
    }
}
