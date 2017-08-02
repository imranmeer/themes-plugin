package com.atex.plugins.themes;

/**
 * Search Plugin Configuration.
 */
public interface ThemesConfiguration {

    int getLineBreakSettings();

    boolean getWarningSettings();

    boolean getNomungeSettings();

    boolean getDebugModeSettings();

    int getCacheTimeSettings();

    boolean getPreserveSemiSettings();

    boolean getCompressCSSSettings();

    boolean getCompressJavascriptSettings();
    
}
