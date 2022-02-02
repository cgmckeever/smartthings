FROM nodered/node-red:latest

USER root

RUN apk add php-cli php7-json php7-session php7-curl && \
    wget https://github.com/Art-of-WiFi/UniFi-API-client/archive/master.zip && \
    unzip master.zip && \
    rm master.zip && \
    mkdir unifi

COPY unifi unifi/

USER node-red
