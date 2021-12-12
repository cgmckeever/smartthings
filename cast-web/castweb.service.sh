#!/bin/bash

set -e

docker run \
  -dit \
  --name castweb \
  --network host \
  --expose=3020 \
  cast-web:latest