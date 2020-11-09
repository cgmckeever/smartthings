FROM ubuntu:latest

ENV PATH /opt/node_app/node_modules/.bin:$PATH

RUN apt-get update && apt-get upgrade -y && \
    DEBIAN_FRONTEND="noninteractive" apt-get install -y npm && \
    npm install --loglevel verbose --no-optional --build-from-resource cast-web-api-cli -g; exit 0 && \
    npm cache clean --force

COPY start.sh .

CMD ["./start.sh"]

