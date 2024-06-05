package hudson.util;

import hudson.EnvVars;
import java.util.List;

public class ProcessProperties {
    public int ppid;
    public EnvVars envVars;
    public List<String> arguments;

    public ProcessProperties(int ppid, EnvVars envVars, List<String> arguments) {
        this.ppid = ppid;
        this.envVars = envVars;
        this.arguments = arguments;
    }
}
