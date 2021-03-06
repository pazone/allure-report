package org.allurefw.report.junit;

import com.google.inject.Inject;
import org.allurefw.report.AttachmentsStorage;
import org.allurefw.report.ResultsReader;
import org.allurefw.report.entity.Attachment;
import org.allurefw.report.entity.Failure;
import org.allurefw.report.entity.LabelName;
import org.allurefw.report.entity.Status;
import org.allurefw.report.entity.TestCaseResult;
import org.allurefw.report.entity.Time;
import org.apache.maven.plugins.surefire.report.ReportTestCase;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.TestSuiteXmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.allurefw.report.ReportApiUtils.generateUid;
import static org.allurefw.report.ReportApiUtils.listFiles;

/**
 * @author Dmitry Baev baev@qameta.io
 *         Date: 14.02.16
 */
public class JunitResultsReader implements ResultsReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JunitResultsReader.class);

    public static final BigDecimal MULTIPLICAND = new BigDecimal(1000);

    private final AttachmentsStorage storage;

    private final TestSuiteXmlParser parser = new TestSuiteXmlParser();

    @Inject
    public JunitResultsReader(AttachmentsStorage storage) {
        this.storage = storage;
    }

    @Override
    public List<TestCaseResult> readResults(Path source) {
        return listFiles(source, "TEST-*.xml")
                .flatMap(this::parse)
                .flatMap(testSuite -> {
                    Path attachmentFile = source.resolve(testSuite.getFullClassName() + ".txt");
                    Optional<Attachment> log = Optional.of(attachmentFile)
                            .filter(Files::exists)
                            .map(storage::addAttachment)
                            .map(attachment -> attachment.withName("System out"));

                    List<TestCaseResult> results = new ArrayList<>();
                    for (ReportTestCase testCase : testSuite.getTestCases()) {
                        TestCaseResult result = convert(testCase);
                        log.ifPresent(result.getAttachments()::add);
                        result.addLabelIfNotExists(LabelName.SUITE, testSuite.getFullClassName());
                        result.addLabelIfNotExists(LabelName.TEST_CLASS, testSuite.getFullClassName());
                        results.add(result);
                    }
                    return results.stream();
                })
                .collect(Collectors.toList());
    }

    protected TestCaseResult convert(ReportTestCase source) {
        TestCaseResult dest = new TestCaseResult();
        dest.setId(String.format("%s#%s", source.getFullClassName(), source.getName()));
        dest.setUid(generateUid());
        dest.setName(source.getName());
        dest.setTime(new Time()
                .withDuration(BigDecimal.valueOf(source.getTime()).multiply(MULTIPLICAND).longValue())
        );
        dest.setStatus(getStatus(source));
        dest.setFailure(getFailure(source));
        return dest;
    }

    protected Status getStatus(ReportTestCase source) {
        if (!source.hasFailure()) {
            return Status.PASSED;
        }
        if ("java.lang.AssertionError".equalsIgnoreCase(source.getFailureType())) {
            return Status.FAILED;
        }
        if ("skipped".equalsIgnoreCase(source.getFailureType())) {
            return Status.CANCELED;
        }
        return Status.BROKEN;
    }

    protected Failure getFailure(ReportTestCase source) {
        if (source.hasFailure()) {
            return new Failure()
                    .withMessage(source.getFailureMessage())
                    .withTrace(source.getFailureDetail());
        }
        return null;
    }

    protected Stream<ReportTestSuite> parse(Path source) {
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(source), UTF_8)) {
            return parser.parse(reader).stream();
        } catch (Exception e) {
            LOGGER.debug("Could not parse result {}: {}", source, e);
            return Stream.empty();
        }
    }
}
