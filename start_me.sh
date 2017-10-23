#!/usr/bin/env bash

ME_DIR=$(dirname $(realpath $0))

cd "$ME_DIR"

. $HOME/.bash_profile

java \
    -Dkotlinx.coroutines.debug \
    -Djava.net.preferIPv4Stack=true \
    -Dkotopunter.settingsFile=deploySettings.json \
    -jar kotopunter-server/target/kotopunter-server-*-SNAPSHOT-fat.jar
