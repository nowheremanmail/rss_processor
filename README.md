A Flink application project using Scala and SBT.

To run and test your application use SBT invoke: 'sbt run'

In order to run your application from within IntelliJ, you have to select the classpath of the 'mainRunner' module in the run/debug configurations.
Simply open 'Run -> Edit configurations...' and then select 'mainRunner' from the "Use classpath of module" dropbox. 


This project tries to read RSS feeds a store into disk using apache flink and scala

To compile and generate jar 

sbt assembly



To Start 

java -cp ~/.ivy2/cache/com.h2database/h2/jars/h2-1.4.196.jar org.h2.tools.Server -webAllowOthers -tcpAllowOthers -baseDir ~/dbs/ &
flink run target/scala-2.11/news-assembly-0.1-SNAPSHOT.jar --rootFolder /home/david/data --pathDB tcp://localhost:9092/~/dbs/news