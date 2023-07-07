#!/bin/bash

TAG=$1
echo "TAG=${TAG}"
set -e
docker login
docker tag $TAG perapoch/tasks-app
docker push perapoch/tasks-app