package com.atex.plugins.themes;

/**
 * @author peterabjohns
 */
/**
 *
 */

import com.polopoly.application.servlet.ApplicationServletUtil;
import com.polopoly.cm.ContentId;
import com.polopoly.cm.ContentIdFactory;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.CmClient;
import com.polopoly.cm.client.CmClientBase;
import com.polopoly.cm.policy.PolicyCMServer;
import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.apache.velocity.tools.view.servlet.ServletToolboxManager;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * Exports the submitted form results.
 *
 * @author Pete Rabjohns
 *
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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        try
        {
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
            if (StringUtils.isBlank(type) ) {
                throw new ServletException("No type specified");
            }

            ContentId cid = ContentIdFactory.createContentId(themeCid);

            ThemeElementPolicy theme = (ThemeElementPolicy) cmServer.getPolicy(cid);
            ArrayList<WebFileResource> files = theme.getFiles (type);
            
            ThemeElementPolicy themebase = theme.getBaseThemePolicy();
            ArrayList < WebFileResource > basefiles = new ArrayList<WebFileResource>();
            if(themebase != null){
            	basefiles = themebase.getFiles(type);
            }
            
            String textType ;
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
        }
        catch (Exception e)
        {
            response.setStatus(500);
            LOG.log(Level.WARNING, "Error during minification for request " + request.getRequestURL(), e);
        } catch (StackOverflowError e) {
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
    private void exportWebFileResources(ArrayList<WebFileResource> files, ThemeElementPolicy theme, ThemeElementPolicy basetheme, PrintWriter out, HttpServletRequest request, HttpServletResponse response) throws CMException, IOException{
    	
    	if(files != null && files.size() > 0){
    		for (WebFileResource f : files) {

    			ByteArrayOutputStream fileout = new ByteArrayOutputStream();
    			theme.exportFile(f.getPath(), fileout);

                if (debugMode) {
                    if (f.isJavaScript()) {
                        LOG.info ("Serving debug Javascript file " + f);
                        out.write(fileout.toString(defaultEncoding));
                    } else {
                        LOG.info ("Serving debug velocity CSS file " + f);
                        translateVelocity(out, fileout.toString(defaultEncoding), theme, basetheme, request, response);
                    }
                } else {
                    if (f.isCss()) {
                        /* Levae this in for reference, but the currently implementaion of the CSS compressor has bugs
                        StringWriter buf = new StringWriter();
                        translateVelocity(buf, fileout.toString(defaultEncoding), theme, basetheme, request, response);
                        fileout.close();
                        out.write (getCompressedCss(buf.getBuffer()));
                        */
                        translateVelocity(out, fileout.toString(defaultEncoding), theme, basetheme, request, response);
                    } else if (f.isJavaScript()) {
                        out.write(getCompressedJavaScript(new ByteArrayInputStream(fileout.toByteArray())));
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
        InputStreamReader isr = new InputStreamReader(inputStream,defaultEncoding);

        JavaScriptCompressor compressor = new JavaScriptCompressor(isr, new CompressorFilterErrorReporter());
        inputStream.close();

        StringWriter out = new StringWriter();
        compressor.compress(out, lineBreakCol, munge, warn, preserveAllSemiColons, disableOptimizations);
        out.flush();

        StringBuffer buffer = out.getBuffer();
        return buffer.toString();
    }

    private void translateVelocity(Writer out, String data, ThemeElementPolicy theme, ThemeElementPolicy basetheme, HttpServletRequest request, HttpServletResponse response) throws IOException, CMException {


        ChainedContext ctx = new ChainedContext((VelocityEngine)null, request, response, getServletContext());

        ctx.setToolbox(toolboxManager.getToolbox(ctx));

        ctx.put("content",  theme);
        ctx.put("basecontent",  basetheme);
        ctx.put("ctx", ctx);

        Velocity.evaluate(ctx, out, "theme-logging", data);
    }

    /**
     *
     * @throws IOException
     */
    private String getCompressedCss(StringBuffer in) throws IOException {
        CssCompressor compressor = new CssCompressor(new StringReader (in.toString()));

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

        String lineBreak = config.getInitParameter("line-break");
        if (lineBreak != null) {
            lineBreakCol = Integer.parseInt(lineBreak);
        }

        String warnString = config.getInitParameter("warn");
        if (warnString != null) {
            warn = Boolean.parseBoolean(warnString);
        }

        String noMungeString = config.getInitParameter("nomunge");
        if (noMungeString != null) {
            munge = Boolean.parseBoolean(noMungeString) ? false : true; //swap values because it's nomunge
        }

        String debugModeString = config.getInitParameter("debugMode");
        if (debugModeString != null) {
            debugMode = Boolean.parseBoolean(debugModeString);
        }

        String cacheTimeString = config.getInitParameter("cache-time");
        if (cacheTimeString != null) {
            cacheTime = Integer.parseInt(cacheTimeString);
        }

        String preserveAllSemiColonsString = config.getInitParameter("preserve-semi");
        if (preserveAllSemiColonsString != null) {
            preserveAllSemiColons = Boolean.parseBoolean(preserveAllSemiColonsString);
        }

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
}
