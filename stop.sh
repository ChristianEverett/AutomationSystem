#!/bin/bash

curl -i \
-H "Accept: application/json" \
-H "Content-Type:application/json" \
-X POST --data '{"device":"shutdown", "data":""}' "http://localhost:8080/action/add" > /dev/null