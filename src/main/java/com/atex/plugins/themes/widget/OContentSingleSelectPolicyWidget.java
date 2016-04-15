package com.atex.plugins.themes.widget;

import com.polopoly.cm.ContentId;
import com.polopoly.cm.app.orchid.widget.OContentSingleSelect;
import com.polopoly.cm.client.CMException;
import com.polopoly.orchid.context.OrchidContext;

public class OContentSingleSelectPolicyWidget extends com.polopoly.cm.app.widget.OContentSingleSelectPolicyWidget {

    private static final long serialVersionUID = 1L;

    public OContentSingleSelect getContentSelect() {
        return contentSelect;
    }

    public void selectContent(ContentId ref, OrchidContext oc) {
        try {
            contentSelect.setSelectedContentId(ref, oc);
        } catch (CMException e) {
            handleError(e, oc);
        }
    }
}
