## 启动命令
-javaagent:D:/codespace/jacocoagent.jar=includes=com.example.*,output=tcpserver,port=6300,address=127.0.0.1

## 下载exec文件
java -jar {jacococli.jar path}/jacococli.jar dump  --port 6300 --destfile {exec目标存放地址}/jacoco-it.exec

java -jar D:/soft/jacoco-0.8.7/lib/jacococli.jar dump  --port 6300 --destfile D:/codespace/data/jacoco-it.exec

## 生成报告

java -jar {jacococli.jar path}/jacococli.jar report {exec 存放地址}/jacoco-it.exec  --sourcefiles {源码路径，com...的路径}  --classfiles {编译文件的路径 com...} --html
{生成报告的文件}

java -jar D:/soft/jacoco-0.8.7/lib/jacococli.jar report D:/codespace/data/jacoco-it.exec  --sourcefiles D:/codespace/idea/testDemo/src/main/java  --classfiles D:/codespace/idea/testDemo/target/classes --html html

