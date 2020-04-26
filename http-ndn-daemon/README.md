This is a program that will translate HTTP traffic for transmission over NDN network.

To change the port used for the proxy, edit the `localHttpPort` found in `ProxyMultiThread.java`. To serve incoming HTTP requests, initialize the `remoteport` in that same file to the port where your server is running.

To build and run:
```
mvn clean install package
java -jar target/http-ndn/ProxyMultiThread.jar
```