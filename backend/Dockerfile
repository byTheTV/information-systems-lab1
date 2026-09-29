FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package dependency:copy -Dartifact=org.postgresql:postgresql:42.7.5 -DoutputDirectory=/build/libs

FROM payara/micro:6.2025.11-jdk21
COPY --from=build --chown=payara:payara /build/target/labwork.war /opt/payara/deployments/labwork.war
COPY --from=build --chown=payara:payara /build/libs/postgresql-42.7.5.jar /opt/payara/postgresql.jar
ENV JVM_ARGS="-Djdk.util.zip.disableZip64ExtraFieldValidation=true -Xmx512m -Duser.timezone=UTC"
CMD ["--deploy", "/opt/payara/deployments/labwork.war", "--contextroot", "/", "--addLibs", "/opt/payara/postgresql.jar"]
