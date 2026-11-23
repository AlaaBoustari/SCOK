# New concepts Learned :

## Docker BuildX vs Build

Docker Buildx Does NOT create the image locally by default and requires a -laod flag.

> docker buildx build --platform linux/amd64 --load -t scok:latest .

    • Standard docker build runs directly inside your local Docker daemon. Everything it does happens right in your local environment.
    • docker buildx runs inside an isolated, separate builder environment (called BuildKit). It does this so it can do advanced things, like building an image for an Intel server (amd64) and an ARM server (arm64) at the exact same time.