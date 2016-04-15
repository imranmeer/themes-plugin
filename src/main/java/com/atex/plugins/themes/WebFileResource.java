package com.atex.plugins.themes;

import com.polopoly.cm.ContentFileInfo;
import org.apache.commons.io.FilenameUtils;

/**
* @author peterabjohns
*/
public class WebFileResource extends ContentFileInfo {

    private String type;

    public WebFileResource(ContentFileInfo f) {
        super(f);
        type = FilenameUtils.getExtension(f.getName()).toLowerCase();
    }

    public String getType() {
        return type;
    }
    public boolean isCss () {
        return type.equals("css");
    }

    public boolean isJavaScript () {
        return type.equals("js");
    }
}
