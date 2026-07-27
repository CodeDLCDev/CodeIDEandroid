#!/usr/bin/env sh

##############################################################################
##
##  Gradle start up script
##
##############################################################################

if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
else
    echo "Error: 'gradle' executable not found in PATH." >&2
    exit 1
fi
