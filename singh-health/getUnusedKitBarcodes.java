import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

class EmailSender {
    public static boolean sendEmailWithAttachment(String from, String password, String to, String smtpHost, String smtpPort, String tls, File attachment) {
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
            message.setSubject("OpenSpecimen: Unused Kit Barcodes Report " + currentDate);

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
            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            System.err.println("Email sending failed: " + e.getMessage());
            return false;
        }
    }
}

class getUnusedBarcodes {
    private static final String QUERY = """
        SELECT 
            COALESCE(site.name, 'Not Specified') AS "Supply Site Name",
            cp.short_title AS "CP Short Title",
            supply_types.name AS "Supply Type Name",
            supply_items.barcode AS "Barcode"
        FROM os_supplies supply
        JOIN os_supply_types supply_types ON supply.type_id = supply_types.identifier
        JOIN catissue_collection_protocol cp ON supply_types.cp_id = cp.identifier
        JOIN os_supply_items supply_items ON supply.identifier = supply_items.supply_id
        LEFT JOIN catissue_site site ON supply.site_id = site.identifier
        WHERE supply_items.used_by IS NULL;
    """;

    public static File generateUnusedKitBarcodesCSV(String user, String password, String host, String dbName) {
        String jdbcUrl = "jdbc:mysql://" + host + ":3306/" + dbName + "?useSSL=false&serverTimezone=UTC";
        File csvFile = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (
                Connection connection = DriverManager.getConnection(jdbcUrl, user, password);
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(QUERY)
            ) {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
                csvFile = new File("unused_specimen_kit_barcodes_" + timestamp + ".csv");
                
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
                    writer.write("\"Supply Site Name\",\"CP Short Title\",\"Supply Type Name\",\"Barcode\"\n");
                    while (resultSet.next()) {
                        writer.write("\"" + resultSet.getString("Supply Site Name") + "\"," +
                                   "\"" + resultSet.getString("CP Short Title") + "\"," +
                                   "\"" + resultSet.getString("Supply Type Name") + "\"," +
                                   "\"" + resultSet.getString("Barcode") + "\"\n");
                    }
                }
            }
        } catch (ClassNotFoundException | SQLException | IOException e) {
            System.err.println("Error: " + e.getMessage());
            return null;
        }
        return csvFile;
    }
}

class ConfigReader {
    private final String configFilePath;
    public ConfigReader(String configFilePath) {
        this.configFilePath = configFilePath;
    }
    public Properties getConfigValues() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(configFilePath)) {
            properties.load(input);
        } catch (IOException e) {
            System.err.println("Error reading config file: " + e.getMessage());
            System.exit(1);
        }
        return properties;
    }
}

public class getUnusedKitBarcodes {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Error: Configuration file not provided. Usage: java getUnusedKitBarcodes <config-file>");
            System.exit(1);
        }

        String configFile = args[0];
        ConfigReader configReader = new ConfigReader(configFile);
        Properties configValues = configReader.getConfigValues();

        String dbUser = configValues.getProperty("dbUser");
        String dbPassword = configValues.getProperty("dbPassword");
        String dbHost = configValues.getProperty("dbHost");
        String dbName = configValues.getProperty("dbName");
        String fromEmailId = configValues.getProperty("fromEmailId");
        String emailPassword = configValues.getProperty("emailPassword");
        String toEmailIds = configValues.getProperty("toEmailIds");
        String smtpServerHost = configValues.getProperty("smtpServerHost");
        String smtpServerPort = configValues.getProperty("smtpServerPort");
        String startTLS = configValues.getProperty("startTLS");

        File csvFile = getUnusedBarcodes.generateUnusedKitBarcodesCSV(dbUser, dbPassword, dbHost, dbName);

        if (csvFile != null) {
            System.out.println("CSV file generated successfully: " + csvFile.getAbsolutePath());

            boolean emailSent = EmailSender.sendEmailWithAttachment(
                fromEmailId, emailPassword, toEmailIds, smtpServerHost, smtpServerPort, startTLS, csvFile);
            if (emailSent) {
                System.out.println("Email sent successfully!");
            } else {
                System.err.println("Failed to send email.");
            }
        } else {
            System.err.println("Failed to generate CSV file.");
        }
    }
}