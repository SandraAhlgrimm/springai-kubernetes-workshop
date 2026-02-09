FROM mcr.microsoft.com/devcontainers/base:bullseye

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        zstd \
    && rm -rf /var/lib/apt/lists/*
