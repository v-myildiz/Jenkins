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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import jenkins.model.Jenkins;
import org.junit.Test;

public class EndOfLifeFedora36AdminMonitorTest {

    private final EndOfLifeFedora36AdminMonitor monitor = new EndOfLifeFedora36AdminMonitor();

    public EndOfLifeFedora36AdminMonitorTest() {
    }

    @Test
    public void testGetIdentifier() {
        assertThat(monitor.getIdentifier(), is("fedora_36"));
    }

    @Test
    public void testGetDependencyName() {
        assertThat(monitor.getDependencyName(), is("Fedora 36"));
    }

    @Test
    public void testGetDisplayName() {
        assertThat(monitor.getDisplayName(), is("End of life for Fedora 36"));
    }

    @Test
    public void testGetBeginDisplayDate() {
        assertThat(monitor.getBeginDisplayDate(), is("2023-03-01"));
    }

    @Test
    public void testGetDocumentationURL() {
        assertThat(monitor.getDocumentationURL(), is("https://www.jenkins.io/redirect/operating-system-end-of-life"));
    }

    @Test
    public void testIsUnsupported() {
        assertThat(monitor.isUnsupported(), is(LocalDate.now().isAfter(LocalDate.of(2023, 5, 16))));
    }

    @Test
    public void testGetRequiredPermission() {
        assertThat(monitor.getRequiredPermission(), is(Jenkins.SYSTEM_READ));
    }

}
