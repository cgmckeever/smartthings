"Dockerfile" 14L, 378B                                                                                                                                                         1,1           All
FROM python:3.8-slim-buster

RUN pip install -I arlo==1.2.61 && \
    mv /usr/local/lib/python3.8/site-packages/arlo.py /usr/local/lib/python3.8/site-packages/arlo.py.install

## master is not working; hotfix
COPY arlo/arlo.py /usr/local/lib/python3.8/site-packages/arlo.py

COPY gmail.credentials .
COPY www/ www/
COPY start.sh .

RUN chmod +x www/cgi-bin/*

CMD ["./start.sh"]
