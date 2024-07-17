#!/bin/bash

transient=""
if [[ ! -z "$3" ]]; then
    data="$(echo $3 | base64)"
    transient="{\"data\": \"$data\"}"
fi

result=$(fablo chaincode invoke $1 "ch1" "privacy" "{\"Args\":$2}" "$transient")
echo $result