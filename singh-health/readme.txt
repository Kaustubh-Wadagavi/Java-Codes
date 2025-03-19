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

##############################################################
# ENCRYPTING PASSWORDS FROM SECURITY FROM THE BROWSER CONSOLE
##############################################################
A. Compile the program
javac PasswordEncryption.java 

B. Running the program
java PasswordEncryption '<Password-to-encrypt>'

C. copy the string and paste in config file. You need to encrypt the email password and db passoword.

#########################################################
# How to run the program in Windows
########################################################
A. Compile the program using the below command:

javac -cp ".;mysql-connector-java-8.0.22.jar;javax.mail-1.6.2.jar;activation-1.1.1.jar" GetUnusedKitBarcodes.java

B. Run the program 

java -cp ".;mysql-connector-java-8.0.22.jar;javax.mail-1.6.2.jar;activation-1.1.1.jar" GetUnusedKitBarcodes config.properties


#########################################################
# How to run the program in Linux
########################################################
A. Compile the program using the below command:
javac -cp ".:mysql-connector-java-8.0.22.jar:javax.mail-1.6.2.jar:activation-1.1.1.jar" GetUnusedKitBarcodes.java

B. Run the program 
java -cp ".:mysql-connector-java-8.0.22.jar:javax.mail-1.6.2.jar:activation-1.1.1.jar" GetUnusedKitBarcodes config.properties

#######################################################
# How to build the jar file.
#######################################################
1. Extract all the dependencies
mkdir temp_lib
cd temp_lib
jar -xf ../mysql-connector-java-8.0.22.jar
jar -xf ../javax.mail-1.6.2.jar
jar -xf ../activation-1.1.1.jar
cd ..

2. Create MANIFEST.MF
Manifest-Version: 1.0
Main-Class: getUnusedKitBarcodes

3. Run the below command to create a jar file.
jar -cvfm getUnusedKitBarcodes.jar MANIFEST.MF *.class -C temp_lib .

###########################################################
# How to run the program using the above jar
###########################################################
java -jar getUnusedKitBarcodes.jar config.properties


