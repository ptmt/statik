#!/bin/sh
set -eu

if [ "${1:-}" = "run" ]; then
    shift
    if [ "${1:-}" = "--" ]; then
        shift
    fi
fi

exec java -jar /app/statik.jar "$@"
