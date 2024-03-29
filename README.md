# Computer Networks Project
---
- Building a P2P file-sharing software like BitTorrent.
- About 800 line for midpoint check on March 10
- Final Submission 4/20

## Catalog
* [Computer Networks Project](#computer-networks-project)
  * [Catalog](#catalog)
  * [How to test?](#how-to-test)
  * [How to manually run on remote server?](#how-to-manually-run-on-remote-server)
  * [Where is the log? How to check?](#where-is-the-log-how-to-check)
  * [Classes](#classes)
      * [5. ActualMsg](#5-actualmsg)
        * [receive msg](#receive-msg)
        * [send msg](#send-msg)
      * [6. FileManager](#6-filemanager)
        * [Block &amp; Bit field functions](#block--bit-field-functions)
        * [File functions](#file-functions)
        * [Other functions](#other-functions)
      * [7. LogHandler](#7-loghandler)
        * [Project description log functions](#project-description-log-functions)
        * [Error log functions](#error-log-functions)
        * [Other log functions](#other-log-functions)


## How to test?
Please follow the cmd below, and use the cmd in script if needed any adjustment.

Before running the script, please put the configurations and the default file directories in the path of 

- **~/final_project/demo/**

If the **debug** in peerProcess is true, the program will it reads **PeerInfo_local.cfg** in the config path.

In this mode, all peers will build locally with different port.

1. compile  

   ```
   sh ~/final_project/compileProcess.sh
   ```

   The java class will be established at  ~/final_project/project/

2. start process

   ```bash
   sh ~/final_project/test/startPeers.sh
   ```

   Wait till all nodes, showing 

3. kill process if needed 

   ```
   sh ~/final_project/test/killAllPeer.sh
   ```


4. check the files result by using diff command or use the script below

   ```
   sh ~/final_project/test/checkFileDiff.sh
   ```

   It should not show any output which means the file are all the same.



## How to run on remote server?

When you login into the server on csie, you could use these command to start the peer manually.

Be aware that the configurations to remain in 

- **~/final_project/{target_file}/**
- **{target_file} = demo** in default, change it if necessary.

1. Compile the project - use script or javac directly
    ```
    sh ~/final_project/compileProcess.sh
    ```

    ```
    javac -Xlint -d [target_file] src/PeerProcess.java src/peer/* src/utils/*
    ```

2. Compile remote deployer  
   ```
   sh ~/final_project/demo/compileDeployer.sh
   ```

3. Start the deployment
   
   ```
   java ~/final_project/demo/StartRemotePeer
   ```
   
4. Kill process on ubuntu if needed

   ```
   ps aux | grep java | grep PeerProces | awk '{print $2}' | xargs /bin/kill
   ```



## Where is the log? How to check?

The log is stored in **~/final_project/{target_file}/log**, with specific file names. The logs are seperate into debug level and info level, which the former one contains every level logs, and the latter one only contains info level (logs request by project description).

- default {target_file} = demo, where we compiled the project

Here is a example, the file name is **debug_log_peer_[1001]_[2021-04-13].log**. With FINE and INFO level logs record in the file.

```
[2021-04-13 12:24:21] [FINE] [utils.LogHandler] Peer [1001] Peer [1001] receive Handshake success from [1008]
[2021-04-13 12:24:21] [FINE] [utils.LogHandler] Peer [1001] Peer [1001] check if [1008] is neighbor
[2021-04-13 12:24:21] [FINE] [utils.LogHandler] Peer [1001] (server thread) # 2 client is connected
[2021-04-13 12:24:21] [INFO] [utils.LogHandler] Peer [1001] (server) sending handshake message to peer [1008]
```

The logs are seperate into 3 files for each node.

- error - with only [SEVERE] Level record inside

- info - with only [INFO] Level record inside

- Debug - with only [Debug] Level record inside

  

## Classes

1. PeerProcess (YiMing)
2. Server (YiMing)
3. Client (YiMing)
4. HandShakeMsg (YiMing)
5. ActualMsg (Jim)
6. FileManager (Jim)
7. LogHandler (YiMing)

### 5. ActualMsg
Class ActualMsg categorize the type of the msg and serialize them into object.

a. noPayloadMsg
   - **end**, **choke**, **unchoke**, **interested** and **not interested**

b. shortMsg
   - **have**, **request** contains 4 bytes payload
   - **have**
     - payload = 4-byte piece index field.
     - sender uses payload to inform receiver it's properties.
   - **request** 
     - payload = 4-byte piece index field
     - receiver request piece x from sender, which x represents by payload.

c. bitfieldMsg

   - **bitfield**
     - contains variable length bitfield
     - every byte is 8 bit which represents 8 different pieces of file.
     - For example, first byte = 3 --> 00000110. (pieces = blockIdx 0 ~ 7)
       Sender contains peices with blockIdx = 1 and 2.

d. pieceMsg
   - **piece**
     - payload consists of a ***4-byte piece index field*** and ***the content
     of the piece.*** 



#### receive msg

| Function Names | input |  return | description |
| ------------- | ------------- | ----------- | ------- |
| recv | InputStream in | byte type | tranform input stream |

#### send msg

| Function Names | input |  return | description |
| ------------- | ------------- | ----------- | ------- |
| send | OutputStream out, byte type, int blockIdx | None | use for noPayloadMsg and shortMsg |
| send | OutputStream out, byte type, byte[] bitfield | None | use for bitfieldMsg |
| send | OutputStream out, byte type, int blockIdx, byte[] data | None | use for pieceMsg |



### 6. FileManager

FileManager is a singleton,  new it as the class's default variables. Then you are able to call it's functions shown in the function list.



#### Block & Bit field functions

| Function Names | input |  return | description  |
| ------------- | ------------- | ----------- | ------- |
| insertBitfield  | int peerId, byte[] b, int len | None | record what peices does **others** have |
| updateHave | String peerId, int blockIdx | None | update **others'** have peices |
| buildOwnBitfield | int remainderBits | None | Build own bit field record in memory |
| updateOwnBitfield | int blockIdx | None | update own bit field |
| getOwnBitfield | | None | get own bit field |
| pickInterestedFileBlock | String peerId | int blockIdx | use for request msg, random pick a interested block which the other peer (peerId) have |
| isInterested | String peerId | boolean | interested in target Peer's blocks |

#### File functions
| Function Names | input |  return | description  |
| ------------- | ------------- | ----------- | ------- |
| read | int blockIdx, byte[] b, int len | int byteRead | read the block, return the length of bytes which just be read |
| write | int blockIdx, byte[] b, int len | int byteWrite | write the block, return the lenght of bytes which just be written |
| close | | None | close the file |

#### Other functions
| Function Names | input |  return | description  |
| ------------- | ------------- | ----------- | ------- |
| isComplete | | None | is peer finishing download the file |
| printByteArray | byte[] bytes | None | print bytes to string |



### 7. LogHandler

- LogHandler is a singleton,  new it as the class's default variables.
  Then you are able to call it's functions shown in the function list.
  
  ```
    LogHandler logging = new LogHandler();
    ...
    logging.writeLog("This is a test log");
  ```

- Please naming the functions with this rule
  ```
    log{Actions}
  ```

#### Project description log functions
---
| Function Names | input |  return | description  |
| ------------- | ------------- | ----------- | ------- |
| logStartConn  | Peer client, Peer targetHost | None | log start connection |
| logChangePrefersPeers  | None  | None | log change of preferred neighbors |
| logChangeUnchokedPeer  | None | None | change of unchoke neighbors |
| logUnchoking  | Peer sender | None | log unchoke targe peer |
| logChoking    | Peer sender | None | log choke target peer  |
| logReceiveHaveMsg  | Peer sender | None | log server send have msg |
| logReceiveInterestMsg  | Peer sender | None | log cleint send interest msg |
| logReceiveNotInterestMsg  | Peer sender | None | log cleint send not interest msg |
| logDownload  | Peer sender, int blockIdx, int numBlocks | None | log download block |
| logCompleteFile  | None | None | log complete downloading the file |
| logCloseConn  | Peer targetPeer | None | log close connection |
| logSendHandShakeMsg | String targetPeerID, String threadType | None | log send hand shake msg |

#### Error log functions
---
| Function Names | input |  return | description  |
| ------------- | ------------- | ----------- | ------- |
| logConnError  | (Peer client, Peer targetHost) | None | Connection Error |
