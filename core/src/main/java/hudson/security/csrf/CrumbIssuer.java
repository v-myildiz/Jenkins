/*
 * Copyright (c) 2008-2009 Yahoo! Inc.
 * All rights reserved.
 * The copyrights to the contents of this file are licensed under the MIT License (http://www.opensource.org/licenses/mit-license.php)
 */
package hudson.security.csrf;

import javax.servlet.ServletRequest;

import hudson.init.Initializer;
import jenkins.model.Jenkins;
import jenkins.security.stapler.StaplerAccessibleType;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.WebApp;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Api;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.MultipartFormDataParser;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerResponse;

/**
 * A CrumbIssuer represents an algorithm to generate a nonce value, known as a
 * crumb, to counter cross site request forgery exploits. Crumbs are typically
 * hashes incorporating information that uniquely identifies an agent that sends
 * a request, along with a guarded secret so that the crumb value cannot be
 * forged by a third party.
 *
 * @author dty
 * @see <a href="http://en.wikipedia.org/wiki/XSRF">Wikipedia: Cross site request forgery</a>
 */
@ExportedBean
@StaplerAccessibleType
public abstract class CrumbIssuer implements Describable<CrumbIssuer>, ExtensionPoint {

    private static final String CRUMB_ATTRIBUTE = CrumbIssuer.class.getName() + "_crumb";

    @Restricted(NoExternalUse.class)
    public static final String DEFAULT_CRUMB_NAME = "Jenkins-Crumb";

    /**
     * Get the name of the request parameter the crumb will be stored in. Exposed
     * here for the remote API.
     */
    @Exported
    public String getCrumbRequestField() {
        return getDescriptor().getCrumbRequestField();
    }

    /**
     * Get a crumb value based on user specific information in the current request.
     * Intended for use only by the remote API.
     */
    @Exported
    public String getCrumb() {
        return getCrumb(Stapler.getCurrentRequest());
    }

    /**
     * Get a crumb value based on user specific information in the request.
     */
    public String getCrumb(ServletRequest request) {
        String crumb = null;
        if (request != null) {
            crumb = (String) request.getAttribute(CRUMB_ATTRIBUTE);
        }
        if (crumb == null) {
            crumb = issueCrumb(request, getDescriptor().getCrumbSalt());
            if (request != null) {
                if (crumb != null && crumb.length() > 0) {
                    request.setAttribute(CRUMB_ATTRIBUTE, crumb);
                } else {
                    request.removeAttribute(CRUMB_ATTRIBUTE);
                }
            }
        }

        return crumb;
    }

    /**
     * Create a crumb value based on user specific information in the request.
     * The crumb should be generated by building a cryptographic hash of:
     * <ul>
     *  <li>relevant information in the request that can uniquely identify the client
     *  <li>the salt value
     *  <li>an implementation specific guarded secret.
     * </ul>
     */
    protected abstract String issueCrumb(ServletRequest request, String salt);

    /**
     * Get a crumb from a request parameter and validate it against other data
     * in the current request. The salt and request parameter that is used is
     * defined by the current configuration.
     */
    public boolean validateCrumb(ServletRequest request) {
        CrumbIssuerDescriptor<CrumbIssuer> desc = getDescriptor();
        String crumbField = desc.getCrumbRequestField();
        String crumbSalt = desc.getCrumbSalt();

        return validateCrumb(request, crumbSalt, request.getParameter(crumbField));
    }

    /**
     * Get a crumb from multipart form data and validate it against other data
     * in the current request. The salt and request parameter that is used is
     * defined by the current configuration.
     */
    public boolean validateCrumb(ServletRequest request, MultipartFormDataParser parser) {
        CrumbIssuerDescriptor<CrumbIssuer> desc = getDescriptor();
        String crumbField = desc.getCrumbRequestField();
        String crumbSalt = desc.getCrumbSalt();

        return validateCrumb(request, crumbSalt, parser.get(crumbField));
    }

    /**
     * Validate a previously created crumb against information in the current request.
     *
     * @param crumb The previously generated crumb to validate against information in the current request
     */
    public abstract boolean validateCrumb(ServletRequest request, String salt, String crumb);

    /**
     * Access global configuration for the crumb issuer.
     */
    @Override
    public CrumbIssuerDescriptor<CrumbIssuer> getDescriptor() {
        return (CrumbIssuerDescriptor<CrumbIssuer>) Jenkins.get().getDescriptorOrDie(getClass());
    }

    /**
     * Returns all the registered {@link CrumbIssuer} descriptors.
     */
    public static DescriptorExtensionList<CrumbIssuer, Descriptor<CrumbIssuer>> all() {
        return Jenkins.get().getDescriptorList(CrumbIssuer.class);
    }

    public Api getApi() {
        return new RestrictedApi(this);
    }

    /**
     * Sets up Stapler to use our crumb issuer.
     */
    @Initializer
    public static void initStaplerCrumbIssuer() {
        WebApp.get(Jenkins.get().servletContext).setCrumbIssuer(new org.kohsuke.stapler.CrumbIssuer() {
            @Override
            public String issueCrumb(StaplerRequest request) {
                CrumbIssuer ci = Jenkins.get().getCrumbIssuer();
                return ci!=null ? ci.getCrumb(request) : DEFAULT.issueCrumb(request);
            }

            @Override
            public void validateCrumb(StaplerRequest request, String submittedCrumb) {
                CrumbIssuer ci = Jenkins.get().getCrumbIssuer();
                if (ci==null) {
                    DEFAULT.validateCrumb(request,submittedCrumb);
                } else {
                    if (!ci.validateCrumb(request, ci.getDescriptor().getCrumbSalt(), submittedCrumb))
                        throw new SecurityException("Crumb didn't match");
                }
            }
        });
    }

    @Restricted(NoExternalUse.class)
    public static class RestrictedApi extends Api {

        RestrictedApi(CrumbIssuer instance) {
            super(instance);
        }

        @Override public void doXml(StaplerRequest req, StaplerResponse rsp, @QueryParameter String xpath, @QueryParameter String wrapper, @QueryParameter String tree, @QueryParameter int depth) throws IOException, ServletException {
            setHeaders(rsp);
            String text;
            CrumbIssuer ci = (CrumbIssuer) bean;
            if ("/*/crumbRequestField/text()".equals(xpath)) { // old FullDuplexHttpStream
                text = ci.getCrumbRequestField();
            } else if ("/*/crumb/text()".equals(xpath)) { // ditto
                text = ci.getCrumb();
            } else if ("concat(//crumbRequestField,\":\",//crumb)".equals(xpath)) { // new FullDuplexHttpStream; Main
                text = ci.getCrumbRequestField() + ':' + ci.getCrumb();
            } else if ("concat(//crumbRequestField,'=',//crumb)".equals(xpath)) { // NetBeans
                if (ci.getCrumbRequestField().startsWith(".") || ci.getCrumbRequestField().contains("-")) {
                    text = ci.getCrumbRequestField() + '=' + ci.getCrumb();
                } else {
                    text = null;
                }
            } else {
                text = null;
            }
            if (text != null) {
                try (OutputStream o = rsp.getCompressedOutputStream(req)) {
                    rsp.setContentType("text/plain;charset=UTF-8");
                    o.write(text.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                super.doXml(req, rsp, xpath, wrapper, tree, depth);
            }
        }

    }

}
