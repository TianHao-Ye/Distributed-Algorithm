# Distributed-Algorithm
## Project Overview
An auction platform based on TOTAL-Order multicast, implement Sequencer-based algorithm and ISIS-based algorithm to achieve TO-Multicast. 

## Project Structure
### client
Client-side project, which refers to auctioneers. Clients can bid products with others.
### server
Server-side project, which refers to the auction platform. Server publish products for bid and show bid results to clients.
### share
In order to allow server and client to use the same serializble Message class to multicast.

## How To Run
Server: 
```
    java -jar server.jar <auction_name> 
```
Client: 
```
    java -jar client.jar <username>
```


