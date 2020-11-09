#!/bin/bash -e

./usr/local/bin/cast-web-api-cli start --port 3020 --hostname 192.168.1.34
tail -f /root/.pm2/logs/cast-web-api-out.log