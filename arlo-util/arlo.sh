#!/bin/bash

set -e

docker run -d -it \
  -p 8000:8000 \
  --name=arlo \
  -e USERNAME='ARLO-USER' \
  -e PASSWORD='ARLO-PASSWORD' \
  arlo:latest