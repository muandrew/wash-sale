#!/bin/bash

cd wash-bazel
export MAVEN_REPO=file:$HOME/.m2/repository; bazel run //kotlin/com/muandrew/stock/cli:lib.publish

