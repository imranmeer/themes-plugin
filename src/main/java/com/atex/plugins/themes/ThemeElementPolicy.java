package com.atex.plugins.themes;

import com.polopoly.cm.ContentId;
import com.polopoly.cm.app.policy.CheckboxPolicy;
import com.polopoly.cm.app.policy.ContentSingleSelectPolicy;
import com.polopoly.cm.app.policy.DuplicatorPolicy;
import com.polopoly.cm.app.policy.FilePolicy;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.policy.Policy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author peterabjohns
 */
public class ThemeElementPolicy extends ElementPolicy {

    private static Logger LOG = Logger.getLogger(ThemeElementPolicy.class.getName());

    public ArrayList<WebFileResource> getFiles(String type) throws CMException, IOException {

        String dupname = type + "dir";
        String duppolicy = type + "file";

        DuplicatorPolicy dup = (DuplicatorPolicy) getChildPolicy(dupname);

        if (dup != null) {
            List duplist = dup.getDuplicatorElements();

            if (duplist != null) {
                ArrayList<WebFileResource> files = new ArrayList<WebFileResource>(duplist.size());

                for (int i = 0; i<duplist.size();i++) {

                    FilePolicy fp = (FilePolicy) ((DuplicatorPolicy.DuplicatorElement)duplist.get(i)).getChildPolicy(duppolicy);
                    String filePath = fp.getFilePath();
                    String fileName = fp.getFileName();

                    if(filePath != null && fileName != null){
                     	String fileFullName = fp.getFilePath() + "/" + fp.getFileName();
                    	files.add (new WebFileResource(fp.getFileInfo(fileFullName)));
                    }
                }
                return files;
            }
        }
        return null;
    }
    
    public ThemeElementPolicy getBaseThemePolicy(){
    	
    	CheckboxPolicy isBasePol;
		try {
			
			isBasePol = (CheckboxPolicy) getChildPolicy("isBase");
			if(!isBasePol.getChecked()){
				
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
