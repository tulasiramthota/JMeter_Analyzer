package org.jmeter.analyzer;
import com.opencsv.CSVReader;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PerformanceAnalyzer {

    private static final String JMETER_HOME = System.getenv("JMETER_HOME");
    private static final String RESULTS_DIR = "results";
    private static final String REPORT_CSV = "consolidated_report.csv";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java -jar analyzer.jar <directory-of-jmx-files>");
            System.exit(1);
        }

        Files.createDirectories(Paths.get(RESULTS_DIR));
        List<MetricSummary> summaries = new ArrayList<>();

        for (String jmxFile : listJmxFiles(args[0])) {
            String baseName = Paths.get(jmxFile).getFileName()
                    .toString()
                    .replaceAll("\\.jmx$", "");
            String jtlPath = RESULTS_DIR + "/" + baseName + ".jtl";

            System.out.println("Running test plan: " + baseName);
            runTestPlan(jmxFile, jtlPath);

            MetricSummary summary = JtlParser.parse(jtlPath, baseName);
            summaries.add(summary);

            // Write individual CSV per test
            String individualCsv = RESULTS_DIR + "/" + baseName + "_metrics.csv";
            ReportGenerator.generateIndividualCsv(summary, individualCsv);
            System.out.println("Metrics for " + baseName + " written to: " + individualCsv);

            // Delay before next run
            System.out.println("Waiting 5 minutes before next run...");
            Thread.sleep(Duration.ofMinutes(5).toMillis());
        }

        // Write consolidated report
        ReportGenerator.generateCsv(summaries, RESULTS_DIR + "/" + REPORT_CSV);
        ReportGenerator.printSummary(summaries);

        System.out.println("Consolidated report written to: " + RESULTS_DIR + "/" + REPORT_CSV);
    }

    private static List<String> listJmxFiles(String dir) throws IOException {
        try (Stream<Path> files = Files.list(Paths.get(dir))) {
            return files
                    .map(Path::toString)
                    .filter(f -> f.endsWith(".jmx"))
                    .collect(Collectors.toList());
        }
    }

    private static void runTestPlan(String jmxPath, String jtlOutput) throws Exception {
        // Determine OS-specific JMeter executable
        String os = System.getProperty("os.name").toLowerCase();
        String exec = os.contains("win") ? "jmeter.bat" : "jmeter";
        String jmeterBin = JMETER_HOME + File.separator + "bin" + File.separator + exec;

        ProcessBuilder pb = new ProcessBuilder(
                jmeterBin,
                "-n",      // non-GUI mode
                "-t", jmxPath,
                "-l", jtlOutput
        );
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("JMeter CLI returned non-zero exit code: " + exitCode);
        }
    }
}

class JtlParser {
    /**
     * Assumes JTL columns:
     * timeStamp(0), elapsed(1), label(2), responseCode(3), ...,
     * success(7), ... URL, Latency(14), IdleTime(15), Connect(16)
     */
    public static MetricSummary parse(String jtlPath, String testName) throws Exception {
        try (CSVReader reader = new CSVReader(new FileReader(jtlPath))) {
            reader.readNext(); // skip header

            List<Long> times = new ArrayList<>();
            List<Long> latencies = new ArrayList<>();
            long count = 0, errors = 0;

            long firstTimestamp = Long.MAX_VALUE;
            long lastTimestamp = Long.MIN_VALUE;

            String[] line;
            while ((line = reader.readNext()) != null) {
                long ts = Long.parseLong(line[0]);
                firstTimestamp = Math.min(firstTimestamp, ts);
                lastTimestamp  = Math.max(lastTimestamp,  ts);

                boolean success = Boolean.parseBoolean(line[7]);
                long elapsed = Long.parseLong(line[1]);
                long latency = Long.parseLong(line[14]);

                if (!success) errors++;
                times.add(elapsed);
                latencies.add(latency);
                count++;
            }

            Collections.sort(times);
            Collections.sort(latencies);

            long minTime    = times.isEmpty() ? 0 : times.get(0);
            long maxTime    = times.isEmpty() ? 0 : times.get(times.size() - 1);
            double avgTime  = times.isEmpty()
                    ? 0
                    : times.stream().mapToLong(Long::longValue).average().orElse(0);

            long minLatency = latencies.isEmpty() ? 0 : latencies.get(0);
            long maxLatency = latencies.isEmpty() ? 0 : latencies.get(latencies.size() - 1);
            double avgLatency = latencies.isEmpty()
                    ? 0
                    : latencies.stream().mapToLong(Long::longValue).average().orElse(0);

            // Throughput = successful requests / total duration (s)
            double durationSec = (lastTimestamp - firstTimestamp) / 1000.0;
            long successful = count - errors;
            double durationMin = durationSec / 60.0;
            double throughput = durationMin > 0
                    ? successful / durationMin
                    : 0;

            return new MetricSummary(
                    testName,
                    count,
                    errors,
                    minTime, maxTime, avgTime,
                    minLatency, maxLatency, avgLatency,
                    throughput
            );
        }
    }
}

class MetricSummary {
    String testName;
    long totalRequests;
    long errorCount;
    long minTime;
    long maxTime;
    double avgTime;
    long minLatency;
    long maxLatency;
    double avgLatency;
    double throughput;

    public MetricSummary(String testName,
                         long totalRequests,
                         long errorCount,
                         long minTime,
                         long maxTime,
                         double avgTime,
                         long minLatency,
                         long maxLatency,
                         double avgLatency,
                         double throughput) {
        this.testName = testName;
        this.totalRequests = totalRequests;
        this.errorCount = errorCount;
        this.minTime = minTime;
        this.maxTime = maxTime;
        this.avgTime = avgTime;
        this.minLatency = minLatency;
        this.maxLatency = maxLatency;
        this.avgLatency = avgLatency;
        this.throughput = throughput;
    }
}

class ReportGenerator {
    public static void generateCsv(List<MetricSummary> sums, String output) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter(output))) {
            writer.println(
                    "TestName,Requests,Errors," +
                            "MinResponseMs,MaxResponseMs,AvgResponseMs," +
                            "MinLatencyMs,MaxLatencyMs,AvgLatencyMs," +
                            "Throughput"
            );
            for (MetricSummary s : sums) {
                writer.printf(
                        "%s,%d,%d,%d,%d,%.2f,%d,%d,%.2f,%.2f%n",
                        s.testName,
                        s.totalRequests,
                        s.errorCount,
                        s.minTime,
                        s.maxTime,
                        s.avgTime,
                        s.minLatency,
                        s.maxLatency,
                        s.avgLatency,
                        s.throughput
                );
            }
        }
    }

    public static void generateIndividualCsv(MetricSummary s, String output) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter(output))) {
            writer.println("Metric,Value");
            writer.printf("TestName,%s%n", s.testName);
            writer.printf("Requests,%d%n", s.totalRequests);
            writer.printf("Errors,%d%n", s.errorCount);
            writer.printf("MinResponseMs,%d%n", s.minTime);
            writer.printf("MaxResponseMs,%d%n", s.maxTime);
            writer.printf("AvgResponseMs,%.2f%n", s.avgTime);
            writer.printf("MinLatencyMs,%d%n", s.minLatency);
            writer.printf("MaxLatencyMs,%d%n", s.maxLatency);
            writer.printf("AvgLatencyMs,%.2f%n", s.avgLatency);
            writer.printf("Throughput,%.2f%n", s.throughput);
        }
    }

    public static void printSummary(List<MetricSummary> sums) {
        System.out.println("\n=== Consolidated Performance Summary ===\n");
        for (MetricSummary s : sums) {
            System.out.printf(
                    "%s -> Req: %d, Err: %d, " +
                            "MinResp: %d ms, MaxResp: %d ms, AvgResp: %.2f ms, " +
                            "MinLat: %d ms, MaxLat: %d ms, AvgLat: %.2f ms, " +
                            "Th: %.2f req/sec%n",
                    s.testName,
                    s.totalRequests,
                    s.errorCount,
                    s.minTime,
                    s.maxTime,
                    s.avgTime,
                    s.minLatency,
                    s.maxLatency,
                    s.avgLatency,
                    s.throughput
            );
        }
        System.out.println("\nInsights:");
        System.out.println("- Identify test plans with high avg response times or latencies.");
        System.out.println("- Investigate error spikes or throughput dips.");
    }
}
