FROM python:3.8-slim-buster

## use version 1.2.58
RUN pip install -I arlo==1.2.58 && \
    cp /usr/local/lib/python3.8/site-packages/arlo.py /usr/local/lib/python3.8/site-packages/arlo.py.install

COPY gmail.credentials .
COPY www/ www/
COPY start.sh .

RUN chmod +x www/cgi-bin/*

CMD ["./start.sh"]
