#!/bin/bash

set -e

docker run -d \
  --net=host \
  --name=homebridge \
  -v $(pwd)/homebridge:/homebridge \
  oznu/homebridge:latest
