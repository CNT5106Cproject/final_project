#!/bin/bash
kill -9 $(ps aux | grep java | grep peer.PeerProces | awk '{print $2}')

# Use for remote server demo
# kill -9 $(ps aux | grep java | grep peer.PeerProces | grep yiming | awk '{print $2}')