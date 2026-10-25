# CI deploy image

Woodpecker runs every step in a fresh container. This image contains the tools
the deploy and provision steps need so they do not install Alpine packages on
each pipeline.

```sh
./vm/ci/build.sh
```

The image is built on the VPS Docker host as `sun-ci:1` and used directly by
Woodpecker. Rebuild it after changing `Dockerfile`.
