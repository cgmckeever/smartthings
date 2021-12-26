#!/bin/bash

set -e

docker run \
  --net=host \
  --name=homebridge 
  -v $(pwd)/homebridge:/homebridge \
  oznu/homebridge:latest
