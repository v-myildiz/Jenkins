package jenkins.mvn;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 * @author Dominik Bartholdi (imod)
 * @since 1.491
 */
public class DefaultSettingsProvider extends SettingsProvider {

    @DataBoundConstructor
    public DefaultSettingsProvider() {
    }

    @Override
    public FilePath supplySettings(Run<?, ?> build, FilePath workspace, TaskListener listener) {
        return null;
    }

    @Extension(ordinal = 99) @Symbol("standard")
    public static class DescriptorImpl extends SettingsProviderDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.DefaultSettingsProvider_DisplayName();
        }
    }
}
