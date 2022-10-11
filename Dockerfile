FROM gcr.io/distroless/java17@sha256:1e9ff3493e32a18bf1dcdbb78a248d90e790b87458cef1f3cd48ad0d6d66fd00

ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseParallelGC -XX:ActiveProcessorCount=2"

COPY build/libs/app.jar /app/
WORKDIR /app
CMD ["app.jar"]
