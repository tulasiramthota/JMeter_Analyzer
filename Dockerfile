# Stage 1: Build analyzer jar
FROM eclipse-temurin:17.0.7_7-jdk-jammy AS builder
WORKDIR /workspace

# Install Maven
RUN apt-get update && apt-get install -y maven && apt-get clean

COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Debugging step: List contents of the target directory
RUN ls -l /workspace/target

# Stage 2: Prepare JMeter
FROM eclipse-temurin:17.0.7_7-jdk-jammy AS jmeter
WORKDIR /opt

# Copy pre-downloaded JMeter and plugins
COPY jmeter/apache-jmeter-5.6.3 /opt/jmeter


# Stage 3: Final image
FROM eclipse-temurin:17.0.7_7-jdk-jammy
WORKDIR /app

# Copy JMeter
COPY --from=jmeter /opt/jmeter /opt/jmeter

# Copy analyzer jar
COPY --from=builder /workspace/target/JMeter_Analyzer-1.0.jar /app/analyzer.jar

# Copy any JMX plans into /app/plans at container build or mount at runtime
# COPY plans /app/plans

ENV JMETER_HOME=/opt/jmeter
ENV RESULTS_DIR=/app/results

# Create results directory
RUN mkdir -p ${RESULTS_DIR}

# Entry point: run analyzer against mounted plans directory
ENTRYPOINT ["sh", "-c", "java -Xmx1g -jar analyzer.jar /app/plans"]