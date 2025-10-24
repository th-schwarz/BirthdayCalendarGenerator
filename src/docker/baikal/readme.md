# Build & Push

docker build -t thschwarz/baikal-it:1.0 .

docker buildx build --platform linux/amd64 -t thschwarz/baikal-it:1.0 --push -f Dockerfile .
