/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
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

package hudson.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeFalse;

import hudson.Functions;
import hudson.model.MultiStageTimeSeries.TimeScale;
import hudson.model.queue.SubTask;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import javax.imageio.ImageIO;
import org.junit.Test;

/**
 * @author Kohsuke Kawaguchi
 */
public class LoadStatisticsTest {

    @Test
    public void graph() throws IOException {
        assumeFalse("TODO: Implement this test on Windows", Functions.isWindows());
        LoadStatistics ls = new LoadStatistics(0, 0) {
            @Override
            public int computeIdleExecutors() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int computeTotalExecutors() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int computeQueueLength() {
                throw new UnsupportedOperationException();
            }

            @Override
            protected Iterable<Node> getNodes() {
                throw new UnsupportedOperationException();
            }

            @Override
            protected boolean matches(Queue.Item item, SubTask subTask) {
                throw new UnsupportedOperationException();
            }
        };

        for (int i = 0; i < 50; i++) {
            ls.onlineExecutors.update(4);
            ls.busyExecutors.update(3);
            ls.availableExecutors.update(1);
            ls.queueLength.update(3);
        }

        for (int i = 0; i < 50; i++) {
            ls.onlineExecutors.update(0);
            ls.busyExecutors.update(0);
            ls.availableExecutors.update(0);
            ls.queueLength.update(1);
        }

        BufferedImage image = ls.createTrendChart(TimeScale.SEC10)
                .renderGraph(new Dimension(400, 400), 1, null);

        File tempFile = File.createTempFile("chart-", "png");
        try (OutputStream os = Files.newOutputStream(tempFile.toPath(), StandardOpenOption.DELETE_ON_CLOSE)) {
            ImageIO.write(image, "PNG", os);
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void isModernWorks() {
        assertThat(LoadStatistics.isModern(Modern.class), is(true));
        assertThat(LoadStatistics.isModern(LoadStatistics.class), is(false));
    }

    private static class Modern extends LoadStatistics {

        protected Modern(int initialOnlineExecutors, int initialBusyExecutors) {
            super(initialOnlineExecutors, initialBusyExecutors);
        }

        @Override
        public int computeIdleExecutors() {
            return 0;
        }

        @Override
        public int computeTotalExecutors() {
            return 0;
        }

        @Override
        public int computeQueueLength() {
            return 0;
        }

        @Override
        protected Iterable<Node> getNodes() {
            return null;
        }

        @Override
        protected boolean matches(Queue.Item item, SubTask subTask) {
            return false;
        }
    }
}
