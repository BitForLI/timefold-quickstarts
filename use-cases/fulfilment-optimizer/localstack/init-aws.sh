#!/usr/bin/env sh
set -eu
awslocal sqs create-queue --queue-name fulfilment-events
