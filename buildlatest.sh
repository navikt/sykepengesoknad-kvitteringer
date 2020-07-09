#!/bin/bash
echo "Bygger flex-bucket-uploader for docker compose utvikling"
./gradlew shadowJar
docker build -t flex-bucket-uploader:latest .
