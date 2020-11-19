/*
 * The MIT License
 *
 * Copyright (c) 2016, Daniel Beck, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.management;

import hudson.Extension;
import hudson.diagnosis.ReverseProxySetupMonitor;
import hudson.model.AdministrativeMonitor;
import hudson.model.PageDecorator;
import hudson.util.HudsonIsLoading;
import hudson.util.HudsonIsRestarting;
import jenkins.diagnostics.URICheckEncodingMonitor;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Show notifications and popups for active administrative monitors on all pages.
 */
@Extension
@Restricted(NoExternalUse.class)
public class AdministrativeMonitorsDecorator extends PageDecorator {
    private final Collection<String> ignoredJenkinsRestOfUrls = new ArrayList<>();

    public AdministrativeMonitorsDecorator() {
        // redundant
        ignoredJenkinsRestOfUrls.add("manage");

        // otherwise this would be added to every internal context menu building request
        ignoredJenkinsRestOfUrls.add("contextMenu");

        // don't show here to allow admins to disable malfunctioning monitors via AdministrativeMonitorsDecorator
        ignoredJenkinsRestOfUrls.add("configure");
    }

    @Override
    public String getDisplayName() {
        return Messages.AdministrativeMonitorsDecorator_DisplayName();
    }


    // Used by API
    public List<AdministrativeMonitor> getNonSecurityAdministrativeMonitors() {
        return getAllActiveAdministrativeMonitors()
                .stream()
                .filter(administrativeMonitor -> !administrativeMonitor.isSecurity())
                .collect(Collectors.toList());
    }

    // Used by Jelly
    public int getNonSecurityAdministrativeMonitorsCount() {
        return getNonSecurityAdministrativeMonitors().size();
    }

    // Used by API
    public List<AdministrativeMonitor> getSecurityAdministrativeMonitors() {
        return getAllActiveAdministrativeMonitors()
                .stream()
                .filter(AdministrativeMonitor::isSecurity)
                .collect(Collectors.toList());
    }

    // Used by Jelly
    public int getSecurityAdministrativeMonitorsCount() {
        return getSecurityAdministrativeMonitors().size();
    }

    private Collection<AdministrativeMonitor> getAllActiveAdministrativeMonitors() {
        Collection<AdministrativeMonitor> active = new ArrayList<>();
        for (AdministrativeMonitor am : Jenkins.get().getActiveAdministrativeMonitors()) {
            if (am instanceof ReverseProxySetupMonitor) {
                // TODO make reverse proxy monitor work when shown on any URL
                continue;
            }
            if (am instanceof URICheckEncodingMonitor) {
                // TODO make URI encoding monitor work when shown on any URL
                continue;
            }
            active.add(am);
        }
        return active;
    }

    /**
     * Compute the administrative monitors that are active and should be shown.
     * This is done only when the instance is currently running and the user has the permission to read them.
     *
     * @return the list of active monitors if we should display them, otherwise null.
     */
    // Used by Jelly
    public Collection<AdministrativeMonitor> getMonitorsToDisplay() {
        if (!Jenkins.get().hasPermission(Jenkins.SYSTEM_READ)) {
            return null;
        }

        StaplerRequest req = Stapler.getCurrentRequest();

        if (req == null) {
            return null;
        }
        List<Ancestor> ancestors = req.getAncestors();

        if (ancestors == null || ancestors.size() == 0) {
            // ???
            return null;
        }

        Ancestor a = ancestors.get(ancestors.size() - 1);
        Object o = a.getObject();

        // don't show while Jenkins is loading
        if (o instanceof HudsonIsLoading || o instanceof HudsonIsRestarting) {
            return null;
        }

        // don't show for some URLs served directly by Jenkins
        if (o instanceof Jenkins) {
            String url = a.getRestOfUrl();

            if (ignoredJenkinsRestOfUrls.contains(url)) {
                return null;
            }
        }

        return getAllActiveAdministrativeMonitors();
    }
}
