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

# Stage 2: Final image
FROM eclipse-temurin:17.0.7_7-jdk-jammy
WORKDIR /app

# Copy analyzer jar
COPY --from=builder /workspace/target/JMeter_Analyzer-1.0.jar /app/analyzer.jar

# Set environment variables
ENV JMETER_HOME=/opt/jmeter
ENV RESULTS_DIR=/app/results

# Create results directory
RUN mkdir -p ${RESULTS_DIR}

# Entry point: run analyzer against mounted plans directory
ENTRYPOINT ["sh", "-c", "java -Xmx1g -jar analyzer.jar /app/plans"]