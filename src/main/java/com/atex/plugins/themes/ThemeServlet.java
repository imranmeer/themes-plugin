package com.atex.plugins.themes;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.javascript.jscomp.*;
import com.google.javascript.jscomp.Compiler;
import com.polopoly.application.servlet.ApplicationServletUtil;
import com.polopoly.cm.ContentId;
import com.polopoly.cm.ContentIdFactory;
import com.polopoly.cm.ExternalContentId;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.CmClient;
import com.polopoly.cm.client.CmClientBase;
import com.polopoly.cm.policy.PolicyCMServer;
import com.yahoo.platform.yui.compressor.CssCompressor;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.apache.velocity.tools.view.servlet.ServletToolboxManager;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Exports the submitted form results.
 *
 * @author Pete Rabjohns
 */
public class ThemeServlet extends HttpServlet {

    private static Logger LOG = Logger.getLogger(ThemeServlet.class.getName());

    private PolicyCMServer cmServer;
    private final String defaultEncoding = "UTF8";

    private static Pattern MINIFIED_FILE = Pattern.compile(".*\\.(min|pack)\\.(css|js)$");

    private ServletToolboxManager toolboxManager;

    public static final String CONFIG_EXTERNALID = "plugins.com.atex.gong.plugins.themes-plugin.Config";
    private final ExternalContentId configId = new ExternalContentId(CONFIG_EXTERNALID);


    private final Supplier<ThemesConfiguration> cachedConfig
            = Suppliers.memoizeWithExpiration(getThemesConfiguration(), 5, TimeUnit.MINUTES);



    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            ThemesConfiguration config = cachedConfig.get();

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

            ThemeElementPolicy theme;

            try {
                String unversionedThemeCid = themeCid.substring(0, themeCid.lastIndexOf("."));
                ContentId unversionedCid = ContentIdFactory.createContentId(unversionedThemeCid);
                theme = (ThemeElementPolicy) cmServer.getPolicy(unversionedCid);
                ContentId latestThemeCid = theme.getContentId();

                if (!latestThemeCid.getContentIdString().equalsIgnoreCase(themeCid)) {
                    response.sendRedirect("/" + parts[0] + "/" + latestThemeCid.getContentIdString() + "/" + parts[2]);
                }
            } catch (Exception e) {
                LOG.warning(e.getMessage());
            }

            ContentId cid = ContentIdFactory.createContentId(themeCid);
            theme = (ThemeElementPolicy) cmServer.getPolicy(cid);
            final List<WebFileResource> files = theme.getFiles(type);

            final ThemeElementPolicy themebase = theme.getBaseThemePolicy();
            final List<WebFileResource> basefiles;
            if (themebase != null) {
                basefiles = themebase.getFiles(type);
            } else {
                basefiles = Lists.newArrayList();
            }

            String textType;
            if (type.endsWith("css")) {
                textType = "css";
            } else {
                textType = "javascript";
            }
            int cacheTime = config.getCacheTimeSettings();
            response.setContentType("text/" + textType + "; charset=UTF-8");
            response.setHeader("Cache-Control", "public, max-age=" + cacheTime + ", s-maxage=" + cacheTime);
            response.setDateHeader("Expires", new Date(System.currentTimeMillis() + cacheTime * 1000).getTime());
            response.setDateHeader("Last-Modified", theme.getContentCreationTime());

            PrintWriter out = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), defaultEncoding), true);

            exportWebFileResources(basefiles, themebase, themebase, out, request, response, config);
            exportWebFileResources(files, theme, themebase, out, request, response, config);

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
    private void exportWebFileResources(List<WebFileResource> files, ThemeElementPolicy theme, ThemeElementPolicy basetheme, PrintWriter out, HttpServletRequest request, HttpServletResponse response, ThemesConfiguration config)
            throws CMException, IOException {

        if (files != null && files.size() > 0) {
            for (WebFileResource f : files) {

                ByteArrayOutputStream fileout = new ByteArrayOutputStream();
                theme.exportFile(f.getPath(), fileout);
                String data = fileout.toString(defaultEncoding)
                                     .replaceAll("url\\s*\\(\"?\\/?(?:[^\\/]+\\/)*?([^\\/]+?\\.[a-z]+).*?\\)", "url('#file({'filename': 'file/$1', 'contentId': \\$content.contentId, '':''})')");

                if (config.getDebugModeSettings()) {
                    if (f.isJavaScript()) {
                        LOG.info("Serving debug Javascript file " + f);
                        out.write(fileout.toString(defaultEncoding));
                    } else {
                        LOG.info("Serving debug velocity CSS file " + f);
                        translateVelocity(out, data, theme, basetheme, request, response);
                    }
                } else {
                    if (f.isCss()) {
                        if (config.getCompressCSSSettings() && !isMinifiedAlready (f.getName())) {
                            StringWriter buf = new StringWriter();
                            translateVelocity(buf, data, theme, basetheme, request, response);
                            fileout.close();
                            out.write(getCompressedCss(buf.getBuffer(), config));
                        } else {
                            translateVelocity(out, data, theme, basetheme, request, response);
                        }
                    } else if (f.isJavaScript()) {
                        if (config.getCompressJavascriptSettings() && !isMinifiedAlready (f.getName())) {
                            out.write(getCompressedJavaScript(f.getName(), new ByteArrayInputStream(fileout.toByteArray()), config));
                        } else {
                            out.write(fileout.toString(defaultEncoding));
                        }
                    }
                }
                out.println();
            }
        }
        out.flush();
    }

    private boolean isMinifiedAlready(String name) {
        return MINIFIED_FILE.matcher(name).matches();
    }


    /**
     * Note that the inputStream is closed!
     *
     * @param inputStream
     * @throws IOException
     */
    private String getCompressedJavaScript(String fileName, InputStream inputStream, ThemesConfiguration config) throws IOException {


        final CompilerOptions options = new CompilerOptions();

        options.setCodingConvention(new ClosureCodingConvention());
        options.setOutputCharset(defaultEncoding);
        options.setWarningLevel(DiagnosticGroups.CHECK_VARIABLES, CheckLevel.WARNING);


        final Compiler compiler = newCompiler(options);
        try {
           final SourceFile[] input = new SourceFile[] {
                    SourceFile.fromInputStream(fileName, inputStream)
            };
            SourceFile[] externs = new SourceFile[] {};

            Result result = compiler.compile(Arrays.asList(externs), Arrays.asList(input), options);
            if (result.success) {
                return compiler.toSource();
            } else {
                return input[0].getCode();
            }
        } catch (final Exception e) {
            LOG.log(Level.WARNING, "Failed to process file " + fileName, e);
        } finally {
            inputStream.close();
        }

        return null;
    }


    private Compiler newCompiler(final CompilerOptions compilerOptions) {
        Compiler.setLoggingLevel(Level.SEVERE);
        final Compiler compiler = new Compiler();

        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(compilerOptions);

        compiler.disableThreads();
        compiler.initOptions(compilerOptions);
        return compiler;
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
    private String getCompressedCss(StringBuffer in, ThemesConfiguration config) throws IOException {
        CssCompressor compressor = new CssCompressor(new StringReader(in.toString()));

        StringWriter out = new StringWriter();
        compressor.compress(out, config.getLineBreakSettings());
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

        toolboxManager = ServletToolboxManager.getInstance(getServletContext(), "/WEB-INF/toolbox.xml");

    }

    /**
     * Turn the policy instance into a simple
     * @return
     */
    private Supplier<ThemesConfiguration> getThemesConfiguration() {
        return () -> getThemesConfiguration(cmServer);
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
