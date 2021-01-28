# Computer Networks Project
---
- Building a P2P file-sharing software like BitTorrent.
- About 800 line for midpoint check on March 10
- Final Submission 4/20

---
## **_Directory_**
   * [Computer Networks Project](#computer-networks-project)
      * [About the system](#about-the-system)
      * [Features](#features)
         * [<strong><em>A. Initialization</em></strong>](#a-initialization)
            * [<strong><em>[TODO] Build the network</em></strong>](#todo-build-the-network)
            * [<strong><em>1. Vairiable Initilization</em></strong> - Read from <strong><em>Common.cfg</em></strong>](#1-vairiable-initilization---read-from-commoncfg)
            * [<strong><em>2. File handling</em></strong> - Read from <strong>PeerInfo.cfg</strong>](#2-file-handling---read-from-peerinfocfg)
            * [<strong><em>3. Logging initialization</em></strong>](#3-logging-initialization)
         * [<strong><em>B. Message Features</em></strong>](#b-message-features)
            * [<strong><em>1. HandShake (32 bytes)</em></strong>](#1-handshake-32-bytes)
            * [<strong><em>2. Message Transmission (8 types of message)</em></strong>](#2-message-transmission-8-types-of-message)
         * [<strong><em>[TODO] C. Neighbor Picking - choke &amp; unchoke mechanism</em></strong>](#todo-c-neighbor-picking---choke--unchoke-mechanism)
## About the system
---
In the real world, people try hard to search for the possibility of downloading files. Using P2P architecture is a great choice, where each peer acts like a repository that can assist the server in distributing the file.

## Features
### **_A. Initialization_**
---
#### **_[TODO] Build the network_**
- Set up Linux Machines
- Build StartRemotePeers.java
   - Read the config
   - Accordingly, initialize the nework and upload the files

#### **_1. Vairiable Initilization_** - Read from **_Common.cfg_**
- Setting intervals and neighbor's limitation for decision making
- File size & Piece size

#### **_2. File handling_** - Read from **PeerInfo.cfg**
Build the peer file by the info
- Peer ID
- Host Name & Port
- flag represent complete file exist

Example -

| Peer ID | HostName | Port | Complete File|
| ---- | ---- | ---- | ---- |
|1001| lin114-05.cise.ufl.edu|5566|1|
|1002| lin114-05.cise.ufl.edu|5566|0|
|1003| lin114-05.cise.ufl.edu|5566|0|

#### **_3. Logging initialization_**
Generate log file in the correct peer, logging the interactions in the network



### **_B. Message Features_**
---
#### **_1. HandShake (32 bytes)_**
- Verify node identity (right neighbor or not)

  |  handshake header   | zero bits  | peer ID|
  |  ----  | ----  | ---- |
  | 18-byte  | 10-byte | 4-byte|
  
  -  handshake header is 18-byte string
‘P2PFILESHARINGPROJ


#### **_2. Message Transmission (8 types of message)_**
- Sending peice infos
- Pick neighbors
- requesting files
- upload peices files

Actual messages packet -

|  message length   | message type | message payload |
|  ----  | ----  | ---- |
| 4-byte  | 1-byte | variable size |

messages type description -
- **_bitfield_** - recieve target chunk infos
- **_have_** - owned the peice
- **_interested & not interested_** - yearn to receive missing chunk or not
- [Todo] **_choke & unchoke_** - decide the number of concurrent upload connections.
- [Todo] **_request & peice_**  
  - request - requesting a piece that the sender does the contain to unchoked neighbor
  - peice - reciever return a peice with the chunk data


- | message type  | value |
  | ---- | ---- |
  |choke |0
  |unchoke |1
  |interested |2
  |not interested |3
  |have |4
  |bitfield |5
  |request |6
  |piece |7
---

### **_[TODO] C. Neighbor Picking - choke & unchoke mechanism_**
