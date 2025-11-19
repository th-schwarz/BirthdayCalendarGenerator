# Build & Push

docker build -t thschwarz/sogo-it:1.0 .

docker buildx build --platform linux/amd64 -t thschwarz/sogo-it:1.0 --push -f Dockerfile .
