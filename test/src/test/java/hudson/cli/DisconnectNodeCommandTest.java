/*
 * The MIT License
 *
 * Copyright 2016 Red Hat, Inc.
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

package hudson.cli;

import static hudson.cli.CLICommandInvoker.Matcher.failedWith;
import static hudson.cli.CLICommandInvoker.Matcher.hasNoStandardOutput;
import static hudson.cli.CLICommandInvoker.Matcher.succeededSilently;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;

import hudson.model.Computer;
import hudson.slaves.DumbSlave;
import hudson.slaves.OfflineCause;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * @author pjanouse
 */
public class DisconnectNodeCommandTest {

    private CLICommandInvoker command;

    @Rule
    public final JenkinsRule j = new JenkinsRule();

    @Before
    public void setUp() {
        command = new CLICommandInvoker(j, "disconnect-node");
    }

    @Test
    public void disconnectNodeShouldFailWithoutComputerDisconnectPermission() throws Exception {
        j.createSlave("aNode", "", null);

        final CLICommandInvoker.Result result = command
                .authorizedTo(Jenkins.READ)
                .invokeWithArgs("aNode");
        assertThat(result, failedWith(6));
        assertThat(result, hasNoStandardOutput());
        assertThat(result.stderr(), containsString("ERROR: user is missing the Agent/Disconnect permission"));
        assertThat(result.stderr(), not(containsString("ERROR: " + CLICommand.CLI_LISTPARAM_SUMMARY_ERROR_TEXT)));
    }

    @Test
    public void disconnectNodeShouldFailIfNodeDoesNotExist() {
        final CLICommandInvoker.Result result = command
                .authorizedTo(Computer.DISCONNECT, Jenkins.READ)
                .invokeWithArgs("never_created");
        assertThat(result, failedWith(3));
        assertThat(result, hasNoStandardOutput());
        assertThat(result.stderr(), containsString("ERROR: No such agent \"never_created\" exists."));
        assertThat(result.stderr(), not(containsString("ERROR: " + CLICommand.CLI_LISTPARAM_SUMMARY_ERROR_TEXT)));
    }

    @Test
    public void disconnectNodeShouldSucceed() throws Exception {
        DumbSlave slave = j.createSlave("aNode", "", null);
        slave.toComputer().waitUntilOnline();
        assertThat(slave.toComputer().isOnline(), equalTo(true));
        assertThat(slave.toComputer().getOfflineCause(), equalTo(null));

        CLICommandInvoker.Result result = command
                .authorizedTo(Computer.DISCONNECT, Jenkins.READ)
                .invokeWithArgs("aNode");
        assertThat(result, succeededSilently());
        assertThat(slave.toComputer().isOffline(), equalTo(true));
        assertThat(slave.toComputer().getOfflineCause(), instanceOf(OfflineCause.ByCLI.class));
        assertThat(((OfflineCause.ByCLI) slave.toComputer().getOfflineCause()).message, equalTo(null));
        assertThat(slave.toComputer().getTemporarilyOfflineCause(), equalTo(null));

        slave.toComputer().connect(true);
        slave.toComputer().waitUntilOnline();
        assertThat(slave.toComputer().isOnline(), equalTo(true));
        assertThat(slave.toComputer().getOfflineCause(), equalTo(null));

        result = command
                .authorizedTo(Computer.DISCONNECT, Jenkins.READ)
                .invokeWithArgs("aNode");
        assertThat(result, succeededSilently());
        assertComputerState(null, slave);

        result = command
                .authorizedTo(Computer.DISCONNECT, Jenkins.READ)
                .invokeWithArgs("aNode");
        assertThat(result, succeededSilently());
        assertComputerState(null, slave);
    }

    @Test
    public void disconnectNodeShouldSucceedWithCause() throws Exception {
        DumbSlave slave = j.createSlave("aNode", "", null);
        slave.toComputer().waitUntilOnline();
        assertThat(slave.toComputer().isOnline(), equalTo(true));
        assertThat(slave.toComputer().getOfflineCause(), equalTo(null));

        CLICommandInvoker.Result result = command
                .authorizedTo(Computer.DISCONNECT, Jenkins.READ)
                .invokeWithArgs("aNode", "-m", "aCause");
        assertThat(result, succeededSilently());
        assertComputerState("aCause", slave);

        slave.toComputer().connect(true);
        slave.toComputer().waitUntilOnline();
        assertThat(slave.toComputer().isOnline(), equalTo(true));
        assertThat(slave.toComputer().getOfflineCause(), equalTo(null));
        assertThat(slave.toComputer().getTemporarilyOfflineCause(), equalTo(null));

        result = command
                .authorizedTo(Computer.DISCONNECT, Jenkins.READ)
                .invokeWithArgs("aNode", "-m", "anotherCause");
        assertThat(result, succeededSilently());
        assertComputerState("anotherCause", slave);

        result = command
                .authorizedTo(Computer.DISCONNECT, Jenkins.READ)
                .invokeWithArgs("aNode", "-m", "yetAnotherCause");
        assertThat(result, succeededSilently());
        assertComputerState("yetAnotherCause", slave);
    }

    @Test
    public void disconnectNodeManyShouldSucceed() throws Exception {
        DumbSlave slave1 = j.createSlave("aNode1", "", null);
        DumbSlave slave2 = j.createSlave("aNode2", "", null);
        DumbSlave slave3 = j.createSlave("aNode3", "", null);
        slave1.toComputer().waitUntilOnline();
        assertThat(slave1.toComputer().isOnline(), equalTo(true));
        assertThat(slave1.toComputer().getOfflineCause(), equalTo(null));
        slave2.toComputer().waitUntilOnline();
        assertThat(slave2.toComputer().isOnline(), equalTo(true));
        assertThat(slave2.toComputer().getOfflineCause(), equalTo(null));
        slave3.toComputer().waitUntilOnline();
        assertThat(slave3.toComputer().isOnline(), equalTo(true));
        assertThat(slave3.toComputer().getOfflineCause(), equalTo(null));

        final CLICommandInvoker.Result result = command
                .authorizedTo(Computer.DISCONNECT, Jenkins.READ)
                .invokeWithArgs("aNode1", "aNode2", "aNode3");
        assertThat(result, succeededSilently());
        assertComputerState(null, slave1);
        assertComputerState(null, slave2);
        assertComputerState(null, slave3);
    }

    @Test
    public void disconnectNodeManyShouldSucceedWithCause() throws Exception {
        DumbSlave slave1 = j.createSlave("aNode1", "", null);
        DumbSlave slave2 = j.createSlave("aNode2", "", null);
        DumbSlave slave3 = j.createSlave("aNode3", "", null);
        slave1.toComputer().waitUntilOnline();
        assertThat(slave1.toComputer().isOnline(), equalTo(true));
        assertThat(slave1.toComputer().getOfflineCause(), equalTo(null));
        slave2.toComputer().waitUntilOnline();
        assertThat(slave2.toComputer().isOnline(), equalTo(true));
        assertThat(slave2.toComputer().getOfflineCause(), equalTo(null));
        slave3.toComputer().waitUntilOnline();
        assertThat(slave3.toComputer().isOnline(), equalTo(true));
        assertThat(slave3.toComputer().getOfflineCause(), equalTo(null));

        final CLICommandInvoker.Result result = command
                .authorizedTo(Computer.DISCONNECT, Jenkins.READ)
                .invokeWithArgs("aNode1", "aNode2", "aNode3", "-m", "aCause");
        assertThat(result, succeededSilently());
        assertComputerState("aCause", slave1);
        assertComputerState("aCause", slave2);
        assertComputerState("aCause", slave3);
    }

    @Test
    public void disconnectNodeManyShouldFailIfANodeDoesNotExist() throws Exception {
        DumbSlave slave1 = j.createSlave("aNode1", "", null);
        DumbSlave slave2 = j.createSlave("aNode2", "", null);
        slave1.toComputer().waitUntilOnline();
        assertThat(slave1.toComputer().isOnline(), equalTo(true));
        assertThat(slave1.toComputer().getOfflineCause(), equalTo(null));
        slave2.toComputer().waitUntilOnline();
        assertThat(slave2.toComputer().isOnline(), equalTo(true));
        assertThat(slave2.toComputer().getOfflineCause(), equalTo(null));

        final CLICommandInvoker.Result result = command
                .authorizedTo(Computer.DISCONNECT, Jenkins.READ)
                .invokeWithArgs("aNode1", "aNode2", "never_created", "-m", "aCause");
        assertThat(result, failedWith(5));
        assertThat(result, hasNoStandardOutput());
        assertThat(result.stderr(), containsString("never_created: No such agent \"never_created\" exists. Did you mean \"aNode1\"?"));
        assertThat(result.stderr(), containsString("ERROR: " + CLICommand.CLI_LISTPARAM_SUMMARY_ERROR_TEXT));
        assertComputerState("aCause", slave1);
        assertComputerState("aCause", slave2);
    }

    @Test
    public void disconnectNodeManyShouldSucceedEvenANodeIsSpecifiedTwice() throws Exception {
        DumbSlave slave1 = j.createSlave("aNode1", "", null);
        DumbSlave slave2 = j.createSlave("aNode2", "", null);
        slave1.toComputer().waitUntilOnline();
        assertThat(slave1.toComputer().isOnline(), equalTo(true));
        assertThat(slave1.toComputer().getOfflineCause(), equalTo(null));
        slave2.toComputer().waitUntilOnline();
        assertThat(slave2.toComputer().isOnline(), equalTo(true));
        assertThat(slave2.toComputer().getOfflineCause(), equalTo(null));

        final CLICommandInvoker.Result result = command
                .authorizedTo(Computer.DISCONNECT, Jenkins.READ)
                .invokeWithArgs("aNode1", "aNode2", "aNode1", "-m", "aCause");
        assertThat(result, succeededSilently());
        assertComputerState("aCause", slave1);
        assertComputerState("aCause", slave2);
    }

    private void assertComputerState(String message, DumbSlave agent)
    {
        Computer computer = agent.toComputer();
        assertThat(computer.isOffline(), equalTo(true));
        assertThat(computer.getOfflineCause(), instanceOf(OfflineCause.ByCLI.class));
        assertThat(computer.getTemporarilyOfflineCause(), equalTo(null));
        assertThat(((OfflineCause.ByCLI) computer.getOfflineCause()).message, equalTo(message));
    }
}
