COMPILE:
    javac TestClient.java Store.java

RUN:
    java Store <mcastip> <mcastport> <nodeip> <nodeport>
            
    java TestClient <node_ap> <operation> [<opnd>]


Examples:

java Store 224.0.0.0 4003 172.0.0.1 8001
java Store 224.0.0.0 4003 172.0.0.2 8002
java TestClient localhost:172.0.0.1 join
java TestClient localhost:172.0.0.1 put ./files/example.txt
java TestClient localhost:172.0.0.1 show
