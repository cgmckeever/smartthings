#!/bin/bash

set -e

docker run -d -it \
  -p 1880:1880 \
  --name=nodered \
  -v /home/pi/node-red:/data \
  nodered:latest