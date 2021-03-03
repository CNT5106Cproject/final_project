# Computer Networks Project
---
- Building a P2P file-sharing software like BitTorrent.
- About 800 line for midpoint check on March 10
- Final Submission 4/20

---
## Functions
1.HandShakeMsg (Mayank)
2.ActualMsg (Jim)
3.Server (YiMing)
4.Client (Mayank)
5.PeerProcess (YiMing)
6.FileManager (Jim)
7.Logging (YiMing)

## 7. Logging
- Typically, define a action function for the system in order to match the project description.
  For example, when 
  ```
     LogHandler logging = new LogHandler();
     logging.startConnect(client, host);

     // Log - "Peer [%s] makes a connection to Peer [%s]"
  ```
  This will write down string with info lvl in the defined log file.

- Use custom write log function to write test log.
  ```
     LogHandler logging = new LogHandler();
    logging.writeLog("This is a test log");
  ```
