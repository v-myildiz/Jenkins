package jenkins.management;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import hudson.model.PageDecorator;
import hudson.model.RootAction;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.verb.GET;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

@Extension
@Restricted(NoExternalUse.class)
public class AdministrativeMonitorsApi implements RootAction {
    @GET
    public void doNonSecurityPopupContent(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
        // TODO: add authorization

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/html");

        AdministrativeMonitorsApiData viewData = new AdministrativeMonitorsApiData(getDecorator().getNonSecurityAdministrativeMonitors());
        req.getView(viewData, "monitorsList.jelly").forward(req, resp);
    }

    @GET
    public void doSecurityPopupContent(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
        // TODO: add authorization


        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/html");

        AdministrativeMonitorsApiData viewData = new AdministrativeMonitorsApiData(getDecorator().getSecurityAdministrativeMonitors());
        req.getView(viewData, "monitorsList.jelly").forward(req, resp);
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return "administrativeMonitors";
    }

    private AdministrativeMonitorsDecorator getDecorator() {
        return Jenkins.get()
                .getExtensionList(PageDecorator.class)
                .get(AdministrativeMonitorsDecorator.class);
    }

    public Collection<AdministrativeMonitor> getMonitorsList() {
        return getDecorator().getMonitorsToDisplay();
    }

}
