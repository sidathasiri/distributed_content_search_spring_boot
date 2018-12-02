# Distributed Content Searching P2P Overlayed Network

This project constructs a simple overlayed p2p file sharing network on top of LAN or Wifi network. 
## How to start
* Start the Bootrstrap server  by running the BootstrapServer file inside the Server directory
* Set the IP and Port of the bootstrap server running node inside the Node class
* Then start a node by running the SpringBootRestApplication file
* You will need to enter a valid port and a username for the current node

##Searching
* search filename
* Ex: search harry potter

##Downloading
* download ip port filename
* Ex: download 192.168.43.157 8000 harry potter