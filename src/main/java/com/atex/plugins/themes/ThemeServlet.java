package com.atex.plugins.themes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.apache.velocity.tools.view.servlet.ServletToolboxManager;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import com.google.common.collect.Lists;
import com.polopoly.application.servlet.ApplicationServletUtil;
import com.polopoly.cm.ContentId;
import com.polopoly.cm.ContentIdFactory;
import com.polopoly.cm.ExternalContentId;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.CmClient;
import com.polopoly.cm.client.CmClientBase;
import com.polopoly.cm.policy.PolicyCMServer;
import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

/**
 * Exports the submitted form results.
 *
 * @author Pete Rabjohns
 */
public class ThemeServlet extends HttpServlet {

    private static Logger LOG = Logger.getLogger(ThemeServlet.class.getName());

    PolicyCMServer cmServer;

    private int lineBreakCol = -1;
    private boolean warn = false;
    private boolean munge = true;
    private boolean preserveAllSemiColons = false;
    private boolean disableOptimizations = false;

    private String defaultEncoding = "UTF8";
    private boolean debugMode = false;

    // Default cache time for 1 year
    private int cacheTime = 60 * 60 * 24 * 365;

    ServletToolboxManager toolboxManager;

    public static final String CONFIG_EXTERNALID = "plugins.com.atex.gong.plugins.themes-plugin.Config";
    final ExternalContentId configId = new ExternalContentId(CONFIG_EXTERNALID);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            defaultEncoding = "UTF8";

            String uri = request.getRequestURI().substring(1);

            String parts[] = uri.split("/");

            if (parts.length < 3 || parts.length > 3) {
                throw new ServletException("Invalid path request");
            }

            String themeCid = parts[1];

            String type = parts[2];

            if (StringUtils.isBlank(themeCid)) {
                throw new ServletException("No theme specified");
            }
            if (StringUtils.isBlank(type)) {
                throw new ServletException("No type specified");
            }

            ContentId cid = ContentIdFactory.createContentId(themeCid);

            final ThemeElementPolicy theme = (ThemeElementPolicy) cmServer.getPolicy(cid);
            final List<WebFileResource> files = theme.getFiles(type);

            final ThemeElementPolicy themebase = theme.getBaseThemePolicy();
            final List<WebFileResource> basefiles;
            if (themebase != null) {
                basefiles = themebase.getFiles(type);
            } else {
                basefiles = Lists.newArrayList();
            }

            String textType;
            if (type.startsWith("css")) {
                textType = "css";
            } else {
                textType = "javascript";
            }
            response.setContentType("text/" + textType + "; charset=UTF-8");
            response.setHeader("Cache-Control", "public, max-age=" + cacheTime + ", s-maxage=" + cacheTime);
            response.setDateHeader("Expires", new Date(System.currentTimeMillis() + cacheTime * 1000).getTime());
            response.setDateHeader("Last-Modified", theme.getContentCreationTime());

            PrintWriter out = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), defaultEncoding), true);

            exportWebFileResources(basefiles, themebase, themebase, out, request, response);
            exportWebFileResources(files, theme, themebase, out, request, response);

            response.setStatus(200);
        } catch (StackOverflowError | Exception e) {
            response.setStatus(500);
            LOG.log(Level.WARNING, "Error during minification for request " + request.getRequestURL(), e);
        }
    }

    /**
     * Export web resources file with the desired theme policy
     *
     * @param files, theme, out
     * @throws CMException, IOException
     */
    private void exportWebFileResources(List<WebFileResource> files, ThemeElementPolicy theme, ThemeElementPolicy basetheme, PrintWriter out, HttpServletRequest request, HttpServletResponse response)
            throws CMException, IOException {

        if (files != null && files.size() > 0) {
            for (WebFileResource f : files) {

                ByteArrayOutputStream fileout = new ByteArrayOutputStream();
                theme.exportFile(f.getPath(), fileout);
                String data = fileout.toString(defaultEncoding)
                                     .replaceAll("url\\s*\\(\"?\\/?(?:[^\\/]+\\/)*?([^\\/]+?\\.[a-z]+).*?\\)", "url('#file({'filename': 'file/$1', 'contentId': \\$content.contentId, '':''})')");

                if (debugMode) {
                    if (f.isJavaScript()) {
                        LOG.info("Serving debug Javascript file " + f);
                        out.write(fileout.toString(defaultEncoding));
                    } else {
                        LOG.info("Serving debug velocity CSS file " + f);
                        translateVelocity(out, data, theme, basetheme, request, response);
                    }
                } else {
                    if (f.isCss()) {
                        /* Leave this in for reference, but the currently implementation of the CSS compressor has bugs
                        StringWriter buf = new StringWriter();
                        translateVelocity(buf, data, theme, basetheme, request, response);
                        fileout.close();
                        out.write (getCompressedCss(buf.getBuffer()));
                        */
                        translateVelocity(out, data, theme, basetheme, request, response);
                    } else if (f.isJavaScript()) {                        
                        /* Leave this in for reference, but the currently implementation of the JS compressor has bugs
                        out.write(getCompressedJavaScript(new ByteArrayInputStream(fileout.toByteArray())));
                        */
                        out.write(fileout.toString(defaultEncoding));
                    }
                }
                out.println();
            }
        }
        out.flush();
    }


    /**
     * Note that the inputStream is closed!
     *
     * @param inputStream
     * @throws IOException
     */
    private String getCompressedJavaScript(InputStream inputStream) throws IOException {
        InputStreamReader isr = new InputStreamReader(inputStream, defaultEncoding);

        JavaScriptCompressor compressor = new JavaScriptCompressor(isr, new CompressorFilterErrorReporter());
        inputStream.close();

        StringWriter out = new StringWriter();
        compressor.compress(out, lineBreakCol, munge, warn, preserveAllSemiColons, disableOptimizations);
        out.flush();

        StringBuffer buffer = out.getBuffer();
        return buffer.toString();
    }

    private void translateVelocity(Writer out, String data, ThemeElementPolicy theme, ThemeElementPolicy basetheme, HttpServletRequest request, HttpServletResponse response)
            throws IOException, CMException {


        ChainedContext ctx = new ChainedContext((VelocityEngine) null, request, response, getServletContext());

        ctx.setToolbox(toolboxManager.getToolbox(ctx));

        ctx.put("content", theme);
        ctx.put("basecontent", basetheme);
        ctx.put("ctx", ctx);

        Velocity.evaluate(ctx, out, "theme-logging", data);
    }

    /**
     * @throws IOException
     */
    private String getCompressedCss(StringBuffer in) throws IOException {
        CssCompressor compressor = new CssCompressor(new StringReader(in.toString()));

        StringWriter out = new StringWriter();
        compressor.compress(out, lineBreakCol);
        out.flush();

        StringBuffer buffer = out.getBuffer();

        return buffer.toString();
    }

    public void destroy() {
    }

    @Override


    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        CmClient cmClient = ((CmClient) ApplicationServletUtil.getApplication(
                getServletContext()).getApplicationComponent(
                CmClientBase.DEFAULT_COMPOUND_NAME));
        cmServer = cmClient.getPolicyCMServer();

        ThemesConfiguration themesConfiguration = getThemesConfiguration(cmClient.getPolicyCMServer());
        lineBreakCol = themesConfiguration.getLineBreakSettings();
        warn = themesConfiguration.getWarningSettings();
        munge = themesConfiguration.getNomungeSettings();
        debugMode = themesConfiguration.getDebugModeSettings();
        cacheTime = themesConfiguration.getCacheTimeSettings();
        preserveAllSemiColons = themesConfiguration.getPreserveSemiSettings();

        toolboxManager = ServletToolboxManager.getInstance(getServletContext(), "/WEB-INF/toolbox.xml");

    }

    public class CompressorFilterErrorReporter implements ErrorReporter {

        public void warning(String message, String sourceName,
                            int line, String lineSource, int lineOffset) {
            if (line < 0) {
                LOG.warning(message);
            } else {
                LOG.warning(line + ':' + lineOffset + ':' + message);
            }
        }

        public void error(String message, String sourceName,
                          int line, String lineSource, int lineOffset) {
            if (line < 0) {
                LOG.warning(message);
            } else {
                LOG.warning(line + ':' + lineOffset + ':' + message);
            }
        }

        public EvaluatorException runtimeError(String message, String sourceName,
                                               int line, String lineSource, int lineOffset) {
            error(message, sourceName, line, lineSource, lineOffset);
            return new EvaluatorException(message);
        }
    }

    ThemesConfiguration getThemesConfiguration(final PolicyCMServer cmServer) {
        try {
            return (ThemesConfigurationPolicy) cmServer.getPolicy(configId);
        } catch (CMException e) {
            LOG.log(Level.SEVERE, "Cannot load configuration from " + CONFIG_EXTERNALID + ": " + e.getMessage(), e);
        }
        return null;
    }
}
