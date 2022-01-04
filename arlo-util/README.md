# Basic Arlo API Server

```
docker build -t arlo:latest -f Dockerfile .
```

```
docker run --rm \
    -p 8000:8000 \
    -e USERNAME='ARLO-USER' \
    -e PASSWORD='ARLO-PASSWORD' \
    -ti arlo:latest
```