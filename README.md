
# JMeter Analyzer

## Overview
JMeter Analyzer is a tool designed to process JMeter `.jtl` files and generate performance metrics such as response times, latencies, error rates, and throughput.

## Prerequisites
- Docker installed on your system
- JMeter binaries available locally

## Usage

### Running the Docker Container
To run the JMeter Analyzer, use the following `docker run` command:

```bash
docker run \
  -v /path/to/local/jmeter:/opt/jmeter \
  -v /path/to/plans:/app/plans \
  -v /path/to/results:/app/results \
  ramdocker23/jmeter-analyzer:latest
```

### Explanation of the Command
- `-v /path/to/local/jmeter:/opt/jmeter`: Mounts the local JMeter directory to the container's `/opt/jmeter` directory.
- `-v /path/to/plans:/app/plans`: Mounts the directory containing `.jmx` test plans to the container's `/app/plans` directory.
- `-v /path/to/results:/app/results`: Mounts the directory where results will be stored to the container's `/app/results` directory.
- `ramdocker23/jmeter-analyzer:latest`: Specifies the Docker image to use.

### Environment Variables
- `JMETER_HOME`: Set to `/opt/jmeter` (mounted JMeter directory).
- `RESULTS_DIR`: Set to `/app/results` (mounted results directory).

### Output
The analyzer processes the `.jmx` test plans and generates performance metrics in the results directory.

## Example
```bash
docker run \
  -v ~/jmeter:/opt/jmeter \
  -v ~/jmeter-plans:/app/plans \
  -v ~/jmeter-results:/app/results \
  ramdocker23/jmeter-analyzer:latest
```

In this example:
- `~/jmeter` contains the local JMeter binaries.
- `~/jmeter-plans` contains the `.jmx` test plans.
- `~/jmeter-results` is where the results will be saved.
```