package org.allurefw.report.core;

import org.allurefw.report.TestRunAggregator;
import org.allurefw.report.TestRunDetailsReader;
import org.allurefw.report.TestRunReader;
import org.allurefw.report.entity.TestRun;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author charlie (Dmitry Baev).
 */
public class ProcessTestRunStage {

    protected final TestRunReader reader;

    protected final Set<TestRunDetailsReader> detailsReaders;

    private final Map<String, TestRunAggregator> aggregators;

    @Inject
    public ProcessTestRunStage(TestRunReader reader, Set<TestRunDetailsReader> detailsReaders,
                               Map<String, TestRunAggregator> aggregators) {
        this.reader = reader;
        this.detailsReaders = detailsReaders;
        this.aggregators = aggregators;
    }

    public Consumer<Map<String, Object>> process(TestRun testRun) {
        return data -> aggregators.forEach((uid, aggregator) -> {
            Object value = data.computeIfAbsent(uid, key -> aggregator.supplier().get());
            //noinspection unchecked
            aggregator.aggregate(testRun).accept(value);
        });
    }

    public Function<Path, TestRun> read() {
        return source -> {
            TestRun testRun = reader.readTestRun(source);
            detailsReaders.forEach(reader -> reader.readDetails(source).accept(testRun));
            return testRun;
        };
    }
}
