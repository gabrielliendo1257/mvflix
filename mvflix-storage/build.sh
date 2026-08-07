#!/bin/bash

set -a
source scripts/dev.env
set +a

mvn spring-boot:run -Dspring.profiles.active=dev
