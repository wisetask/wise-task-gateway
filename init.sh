#! /bin/bash

cd ..
git clone git@github.com:Terross/wise-task-task.git
git clone git@github.com:Terross/wise-task-profile.git
git clone git@github.com:Terross/wise-task-graph.git
git clone git@github.com:Terross/wise-task-plugin.git
git clone git@github.com:magofrays/wise-task-event.git

cd wise-task-gateway
cd src/main/resources
mkdir certs
cd certs

openssl genpkey -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:2048 # приватный ключ
openssl rsa -in private.pem -pubout -out public.pem # публичный ключ
cp public.pem ../../../../../wise-task-event/src/main/resources/certs/public.pem # копируем публичный ключ в wise-task-event
cd ../../../..

docker compose up --build
