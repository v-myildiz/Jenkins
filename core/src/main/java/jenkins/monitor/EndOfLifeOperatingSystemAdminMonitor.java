/*
 * The MIT License
 *
 * Copyright 2023 Mark Waite.
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

package jenkins.monitor;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import hudson.security.Permission;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

@Extension
@Restricted(NoExternalUse.class)
@Symbol("operatingSystemEndOfLife")
public class EndOfLifeOperatingSystemAdminMonitor extends AdministrativeMonitor {

    /** Allow tests to disable the end of life monitor without a JenkinsRule. */
    boolean ignoreEndOfLife = false;

    private boolean afterStartDate = false;
    private String prettyName = "unrecognized operating system";
    private String endOfLifeDate = "unknown date";

    private static class EndOfLifeData {

        final String prettyName;
        final LocalDate startDate;
        final LocalDate effectiveDate;

        EndOfLifeData(String prettyName, LocalDate startDate, LocalDate effectiveDate) {
            this.prettyName = prettyName;
            this.startDate = startDate;
            this.effectiveDate = effectiveDate;
        }
    }

    private final List<EndOfLifeData> operatingSystemList = new ArrayList<>();

    public EndOfLifeOperatingSystemAdminMonitor(String id) throws IOException {
        super(id);
        fillOperatingSystemList();
    }

    public EndOfLifeOperatingSystemAdminMonitor() throws IOException {
        fillOperatingSystemList();
    }

    private void fillOperatingSystemList() throws IOException {
        if (Jenkins.getInstanceOrNull() != null && !isEnabled()) {
            /* If not enabled, do not read the data files or perform any checks */
            return;
        }
        LocalDate now = LocalDate.now();
        ClassLoader cl = getClass().getClassLoader();
        URL localOperatingSystemData = cl.getResource("jenkins/monitor/EndOfLifeOperatingSystemAdminMonitor/end-of-life-data.json");
        String initialOperatingSystemJson = IOUtils.toString(localOperatingSystemData.openStream(), StandardCharsets.UTF_8);
        JSONArray systems = JSONArray.fromObject(initialOperatingSystemJson);
        for (Object systemObj : systems) {
            if (!(systemObj instanceof JSONObject)) {
                LOGGER.log(Level.SEVERE, "Wrong object type in operating system end of life monitor data file");
                break;
            }
            JSONObject system = (JSONObject) systemObj;

            if (!system.has("pattern")) {
                LOGGER.log(Level.SEVERE, "No pattern to be matched in operating system end of life monitor");
                break;
            }
            String pattern = system.getString("pattern");

            if (!system.has("start")) {
                LOGGER.log(Level.SEVERE, "No start date for operating system in end of life monitor for pattern {0}", pattern);
                break;
            }
            LocalDate startDate = LocalDate.parse(system.getString("start"));

            if (!system.has("effective")) {
                LOGGER.log(Level.SEVERE, "No effective date for operating system in end of life monitor for pattern {0}", pattern);
                break;
            }
            LocalDate effectiveDate = LocalDate.parse(system.getString("effective"));

            LOGGER.log(Level.FINE, "Pattern {0} starts {1} and is effective {2}",
                    new Object[]{pattern, startDate, effectiveDate});

            File dataFile;
            if (!system.has("file")) {
                dataFile = new File("/etc/os-release");
            } else {
                dataFile = new File(system.getString("file"));
            }

            String operatingSystemName = readPrettyName(dataFile, pattern);
            if (!operatingSystemName.isEmpty()) {
                operatingSystemList.add(new EndOfLifeData(operatingSystemName, startDate, effectiveDate));
                if (startDate.isBefore(now)) {
                    afterStartDate = true;
                    this.prettyName = operatingSystemName;
                    this.endOfLifeDate = effectiveDate.toString();
                }
            }
        }

    }

    /* Package protected for testing */
    @NonNull
    String readPrettyName(File dataFile, String patternStr) {
        if (!dataFile.exists()) {
            return "";
        }
        Pattern pattern = Pattern.compile("^PRETTY_NAME=[\"](" + patternStr + ".*)[\"]");
        String prettyName = "";
        try {
            List<String> lines = Files.readAllLines(dataFile.toPath());
            for (String line : lines) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    prettyName = matcher.group(1);
                }
            }
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "File read exception", ioe);
        }
        return prettyName;
    }

    @Override
    public boolean isActivated() {
        if (ignoreEndOfLife) {
            LOGGER.log(Level.FINE, "Not activated because ignoring end of life monitor");
            return false;
        }
        if (!afterStartDate) {
            LOGGER.log(Level.FINE, "Not activated because it is before the start date");
            return false;
        }
        return true;
    }

    @Override
    public Permission getRequiredPermission() {
        return Jenkins.SYSTEM_READ;
    }

    @Override
    public String getDisplayName() {
        return "Operating system end of life monitor";
    }

    /**
     * Depending on whether the user said "yes" or "no", send him to the right
     * place.
     */
    @Restricted(DoNotUse.class) // WebOnly
    @RequirePOST
    public HttpResponse doAct(@QueryParameter String no) throws IOException {
        if (no != null) { // dismiss
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            disable(true);
            LOGGER.log(Level.FINE, "Disabled operating system end of life monitor");
            return HttpResponses.forwardToPreviousPage();
        } else {
            LOGGER.log(Level.FINE, "Enabled operating system end of life monitor");
            return new HttpRedirect("https://www.jenkins.io/redirect/operating-system-end-of-life");
        }
    }

    @NonNull
    public String getPrettyName() {
        return prettyName;
    }

    @NonNull
    public String getEndOfLifeDate() {
        return endOfLifeDate;
    }

    static final Logger LOGGER = Logger.getLogger(EndOfLifeOperatingSystemAdminMonitor.class.getName());
}
