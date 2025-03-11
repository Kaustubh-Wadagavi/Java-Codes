#######################################################
# Configuring pre-requisites
#######################################################
Open the config.properties and add the below values and save the file.

dbUser = <Database user name>
dbPassword = <Database user Password>
dbHost = <Database host name>
dbName = <Database Name>
fromEmailId = <Email ID which is going to send the emails after generating the report>
emailPassword = <Email Password>
toEmailIds = <Comma separated email ids>
smtpServerHost = <Email server host>
smtpServerPort = <Email Server Port>
startTLS = <Enable if StartTLS is to be used for your email server>  <values: true/false>

#########################################################
# Unzip the dependencies
#########################################################
1. Download the dependencies.zip file and unzip in the same directory.
2. Three extracted jar files should be present in the same program directory: activation-1.1.1.jar,javax.mail-1.6.2.jar .mysql-connector-java-8.0.22.jar

#########################################################
# How to run the program in Windows
########################################################
A. Compile the program using the below command:

javac -cp ".;mysql-connector-java-8.0.22.jar;javax.mail-1.6.2.jar;activation-1.1.1.jar" getUnusedKitBarcodes.java

B. Run the program 

java -cp ".;mysql-connector-java-8.0.22.jar;javax.mail-1.6.2.jar;activation-1.1.1.jar" getUnusedKitBarcodes config.properties


#########################################################
# How to run the program in Linux
########################################################
A. Compile the program using the below command:

javac -cp ".:mysql-connector-java-8.0.22.jar:javax.mail-1.6.2.jar:activation-1.1.1.jar" getUnusedKitBarcodes.java

B. Run the program 

java -cp ".:mysql-connector-java-8.0.22.jar:javax.mail-1.6.2.jar:activation-1.1.1.jar" getUnusedKitBarcodes config.properties

