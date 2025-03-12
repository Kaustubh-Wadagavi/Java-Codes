import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Multipart;
import java.io.FileInputStream;

class CustomLogger {
    public static Logger createLogger(String className) {
        Logger logger = Logger.getLogger(className);
        logger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter() {
            private static final String FORMAT = "%1$tF %1$tT [%2$s] %3$s%n";
            @Override
            public synchronized String format(LogRecord record) {
                return String.format(FORMAT,
                        new java.util.Date(record.getMillis()),
                        record.getLevel(),
                        record.getMessage()
                );
            }
        });
        logger.addHandler(handler);
        return logger;
    }
}

class EmailSender {
    private static final Logger logger = CustomLogger.createLogger(EmailSender.class.getName());
    
    public static boolean sendEmailWithAttachment(String from, String password, String to, String smtpHost, String smtpPort, String environment, String tls, File attachment) {
        logger.info("Preparing to send email...");
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", tls);

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
            message.setSubject("[OpenSpecimen/" + environment + "]: Unused Kit Barcodes Report " + currentDate);

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText("Hello,\n\n" +
                "Please find the attached unused kit barcodes report generated on: " + currentDate + "\n\n" +
                "Thanks,\n" +
                "OpenSpecimen Administrator");

            MimeBodyPart attachmentPart = new MimeBodyPart();
            DataSource source = new FileDataSource(attachment);
            attachmentPart.setDataHandler(new DataHandler(source));
            attachmentPart.setFileName(attachment.getName());

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            multipart.addBodyPart(attachmentPart);

            message.setContent(multipart);
            logger.info("Sending email...");
            Transport.send(message);
            logger.info("Email sent successfully!");
            return true;
        } catch (MessagingException e) {
            logger.log(Level.SEVERE, "Email sending failed: {0}", e.getMessage());
            return false;
        }
    }
}

class getUnusedBarcodes {
    private static final Logger logger = CustomLogger.createLogger(getUnusedBarcodes.class.getName());

    private static final String QUERY = """
    SELECT
    	COALESCE(site.name, 'Not Specified') AS "Supply Site Name",
  	cp.short_title AS "CP Short Title",
  	supply_types.name AS "Supply Type Name",
  	supply_items.barcode AS "Barcode",
  	supply.creation_time AS "Created On"
    FROM os_supplies supply
  	JOIN os_supply_types supply_types ON supply.type_id = supply_types.identifier
  	JOIN catissue_collection_protocol cp ON supply_types.cp_id = cp.identifier
  	JOIN os_supply_items supply_items ON supply.identifier = supply_items.supply_id
  	LEFT JOIN catissue_site site ON supply.site_id = site.identifier
    WHERE supply_items.used_by IS NULL
	and supply_items.entity_type = 'visit'
	order by supply.creation_time desc;
    """;

    public static File generateUnusedKitBarcodesCSV(String user, String password, String host, String dbName) {
        String jdbcUrl = "jdbc:mysql://" + host + ":3306/" + dbName + "?useSSL=false&serverTimezone=UTC";
        File csvFile = null;

        try {
            logger.info("Connecting to database...");
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (
                Connection connection = DriverManager.getConnection(jdbcUrl, user, password);
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(QUERY)
            ) {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
                csvFile = new File("unused_specimen_kit_barcodes_" + timestamp + ".csv");
                logger.info("Generating CSV file: " + csvFile.getAbsolutePath());

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
                    writer.write("\"Supply Site Name\",\"CP Short Title\",\"Supply Type Name\",\"Barcode\",\"Created On\"\n");
                    int recordCount = 0;
                    while (resultSet.next()) {
                        writer.write("\"" + resultSet.getString("Supply Site Name") + "\"," +
                                   "\"" + resultSet.getString("CP Short Title") + "\"," +
                                   "\"" + resultSet.getString("Supply Type Name") + "\"," +
                                   "\"" + resultSet.getString("Barcode") + "\"," +
                                   "\"" + resultSet.getString("Created On") + "\"\n");
                        recordCount++;
                    }
                    logger.info("CSV generation complete. Records written: " + recordCount);
                }
            }
        } catch (ClassNotFoundException | SQLException | IOException e) {
            logger.log(Level.SEVERE, "Error: {0}", e.getMessage());
            return null;
        }
        return csvFile;
    }
}

public class getUnusedKitBarcodes {
    private static final Logger logger = CustomLogger.createLogger(getUnusedKitBarcodes.class.getName());
    
    public static void main(String[] args) {
        if (args.length == 0) {
            logger.severe("Error: Configuration file not provided. Usage: java getUnusedKitBarcodes <config-file>");
            System.exit(1);
        }

        logger.info("Reading configuration file...");
        String configFile = args[0];
        Properties configValues = new Properties();
        try (FileInputStream input = new FileInputStream(configFile)) {
            configValues.load(input);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading config file: {0}", e.getMessage());
            System.exit(1);
        }

        logger.info("Generating unused kit barcodes report...");
        File csvFile = getUnusedBarcodes.generateUnusedKitBarcodesCSV(configValues.getProperty("dbUser"),
                configValues.getProperty("dbPassword"),
                configValues.getProperty("dbHost"),
                configValues.getProperty("dbName"));
        if (csvFile != null) {
            logger.info("Report generated successfully. Sending email...");
            EmailSender.sendEmailWithAttachment(configValues.getProperty("fromEmailId"),
                    configValues.getProperty("emailPassword"),
                    configValues.getProperty("toEmailIds"),
                    configValues.getProperty("smtpServerHost"),
                    configValues.getProperty("smtpServerPort"),
		    configValues.getProperty("environment"),
                    configValues.getProperty("startTLS"), csvFile);
        }
    }
}