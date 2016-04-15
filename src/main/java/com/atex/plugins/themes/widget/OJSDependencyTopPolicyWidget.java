package com.atex.plugins.themes.widget;

import com.polopoly.cm.app.widget.OTopPolicyWidget;
import com.polopoly.cm.policy.PolicyUtil;
import com.polopoly.common.lang.StringUtil;
import com.polopoly.orchid.OrchidException;
import com.polopoly.orchid.context.OrchidContext;
import com.polopoly.orchid.js.HasJSDependencies;

public class OJSDependencyTopPolicyWidget extends OTopPolicyWidget implements HasJSDependencies {

    private String[] jsFilePath = new String[1];

    @Override
    public void initSelf(OrchidContext orchidcontext) throws OrchidException {
        super.initSelf(orchidcontext);
        String filePath = PolicyUtil.getParameter("jsFilePath", getPolicy());
        if (!StringUtil.isEmpty(filePath)) {
            jsFilePath = new String[] { filePath };
        }
    }

    @Override
    public String[] getJSScriptDependencies() {
        if (jsFilePath != null) {
            return jsFilePath;
        }
        return new String[] {};

    }
}