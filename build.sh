#! /bin/bash
echo "Input service name"
read service

docker compose up --build -d $service

docker image prune -f