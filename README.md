To run:
```
mvn clean install package
java -cp target/http-ndn-daemon-1.0.0.jar \    
com.ndn.http.ProxyMultiThread 127.0.0.1 80 9999
```