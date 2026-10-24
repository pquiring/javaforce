#!/bin/bash
TEST=$1
shift
$HOME/bin/jfexec -cp javaforce.jar javaforce.tests.Test$TEST $*
