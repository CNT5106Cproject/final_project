#!/bin/bash
kill -9 $(netstat -vanp tcp | grep 556 | awk '{print $9}')