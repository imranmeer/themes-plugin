package com.atex.plugins.themes;

import com.atex.onecms.content.ContentManager;
import com.atex.plugins.baseline.policy.BaselinePolicy;
import com.polopoly.cm.app.policy.RadioButtonPolicy;
import com.polopoly.cm.app.policy.SingleValuePolicy;
import com.polopoly.cm.client.CMException;
import com.polopoly.model.DescribesModelType;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Policy that provides the plugin configuration.
 */
@DescribesModelType
public class ThemesConfigurationPolicy extends BaselinePolicy implements ThemesConfiguration {

    private static final Logger LOGGER = Logger.getLogger(ThemesConfigurationPolicy.class.getName());

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
    public int getLineBreakSettings(){
        int result = 0;

        try{
            result = Integer.parseInt(((SingleValuePolicy)getChildPolicy("line-break")).getValue());;
        }catch (CMException e){
            LOGGER.log(Level.SEVERE, "Cannot parse field : " + e.getMessage(), e);
        }

        return result;
    }

    @Override
    public boolean getWarningSettings() {
        boolean result = false;

        try{
            String settings = ((RadioButtonPolicy)getChildPolicy("warn")).getValue();
            if(settings.equals("yes")){
                result = true;
            }else{
                result = false;
            }
        }catch (CMException e){
            LOGGER.log(Level.SEVERE, "Cannot parse field : " + e.getMessage(), e);
        }

        return result;
    }

    @Override
    public boolean getNomungeSettings() {
        boolean result = false;

        try{
            String settings = ((RadioButtonPolicy)getChildPolicy("nomunge")).getValue();
            if(settings.equals("yes")){
                result = true;
            }else{
                result = false;
            }
        }catch (CMException e){
            LOGGER.log(Level.SEVERE, "Cannot parse field : " + e.getMessage(), e);
        }

        return result;
    }

    @Override
    public boolean getDebugModeSettings() {
        boolean result = false;

        try{
            String settings = ((RadioButtonPolicy)getChildPolicy("debugMode")).getValue();
            if(settings.equals("yes")){
                result = true;
            }else{
                result = false;
            }
        }catch (CMException e){
            LOGGER.log(Level.SEVERE, "Cannot parse field : " + e.getMessage(), e);
        }

        return result;
    }

    @Override
    public int getCacheTimeSettings() {
        int result = 0;

        try{
            result = Integer.parseInt(((SingleValuePolicy)getChildPolicy("cache-time")).getValue());;
        }catch (CMException e){
            LOGGER.log(Level.SEVERE, "Cannot parse field : " + e.getMessage(), e);
        }

        return result;
    }

    @Override
    public boolean getPreserveSemiSettings() {
        boolean result = false;

        try{
            String settings = ((RadioButtonPolicy)getChildPolicy("preserve-semi")).getValue();
            if(settings.equals("yes")){
                result = true;
            }else{
                result = false;
            }
        }catch (CMException e){
            LOGGER.log(Level.SEVERE, "Cannot parse field : " + e.getMessage(), e);
        }

        return result;
    }

}
