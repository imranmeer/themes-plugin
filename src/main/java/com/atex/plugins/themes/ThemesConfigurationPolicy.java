package com.atex.plugins.themes;

import com.atex.onecms.content.ContentManager;
import com.atex.plugins.baseline.policy.BaselinePolicy;
import com.google.common.base.Optional;
import com.polopoly.model.DescribesModelType;

/**
 * Policy that provides the plugin configuration.
 */
@DescribesModelType
public class ThemesConfigurationPolicy extends BaselinePolicy implements ThemesConfiguration {

    private ContentManager contentManager;

    /**
     * Constructor.
     *
     * @param contentManager the content manager, automatically provided by Polopoly.
     */
    public ThemesConfigurationPolicy(final ContentManager contentManager) {
        this.contentManager = contentManager;
    }


    @Override
    public int getLineBreakSettings() {
        return Integer.parseInt(getSingleValue("line-break", "0"));
    }

    @Override
    public boolean getWarningSettings() {
        return getSingleValue("warn", "").equals("yes");
    }

    @Override
    public boolean getNomungeSettings() {
        return getSingleValue("nomunge", "").equals("yes");
    }

    @Override
    public boolean getDebugModeSettings() {
        return getSingleValue("debugMode", "").equals("yes");
    }

    @Override
    public int getCacheTimeSettings() {
        return Integer.parseInt(getSingleValue("cache-time", "0"));
    }

    @Override
    public boolean getPreserveSemiSettings() {
        return getSingleValue("preserve-semi", "").equals("yes");
    }

    @Override
    public boolean getCompressCSSSettings() {
        return getSingleValue("compressCSS", "").equals("yes");
    }

    @Override
    public boolean getCompressJavascriptSettings() {
            return getSingleValue("compressJavascript", "").equals("yes");
    }

    private String getSingleValue(final String name, final String defaultValue) {
        return Optional
                .fromNullable(getChildValue(name, defaultValue))
                .or(defaultValue);
    }

}
