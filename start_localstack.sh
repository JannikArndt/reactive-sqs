#!/bin/bash

LOCALSTACK_DOCKER_CONTAINER_NAME="localstack"

echo "Starting SQS via localstack.."

if [ ! "$(docker ps -q -f name=${LOCALSTACK_DOCKER_CONTAINER_NAME})" ]; then
    if [[ "$(docker ps -aq -f status=exited -f name=${LOCALSTACK_DOCKER_CONTAINER_NAME})" ]]; then
        # cleanup
        docker rm -f ${LOCALSTACK_DOCKER_CONTAINER_NAME}
    fi
    # run your container
    docker run -d --env SERVICES="sqs" --env TMPDIR="/tmp" \
        --name ${LOCALSTACK_DOCKER_CONTAINER_NAME} \
        --publish 4576:4576 \
        --publish 8080:8080 \
        --rm localstack/localstack
    echo "Localstack started."
else
    echo "Localstack already started."
fi

aws --endpoint-url=http://localhost:4576 sqs create-queue --queue-name myqueue

aws --endpoint-url=http://localhost:4576 sqs send-message --queue-url "http://localhost:4576/queue/myqueue" --message-body "Hallo"