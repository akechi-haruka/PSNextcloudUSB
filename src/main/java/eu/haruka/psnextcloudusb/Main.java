package eu.haruka.psnextcloudusb;

import org.aarboard.nextcloud.api.AuthenticationConfig;
import org.aarboard.nextcloud.api.NextcloudConnector;

import java.io.File;
import java.io.FileReader;
import java.util.Date;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static final String NAME = "PSNextcloudUSB/1.0 - 2026 Haruka";
    public static final String ROOT_DIR = "/opt/psnextcloudusb/";
    public static final Logger logger = Logger.getLogger(Main.class.getName());

    public static NextcloudConnector nextcloud;
    public static Properties config;

    public static void main(String[] args) throws Exception {
        config = new Properties();
        config.load(new FileReader("/boot/psnextcloudusb/config.properties"));

        Level l = Level.parse(config.getProperty("loglevel", "INFO"));
        logger.setLevel(l);
        logger.setUseParentHandlers(false);
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(l);
        logger.addHandler(ch);

        logger.info(NAME);
        logger.info("Log level is " + logger.getLevel());
        logger.fine(new Date() + " / " + System.currentTimeMillis());

        String url = config.getProperty("url");
        nextcloud = new NextcloudConnector(url,
                config.getProperty("login_method", "password").equals("password") ?
                        new AuthenticationConfig(config.getProperty("username"), config.getProperty("password"))
                        : new AuthenticationConfig(config.getProperty("token")));
        if (config.getProperty("ignore_certificates", "false").equals("true")){
            nextcloud.trustAllCertificates(true);
        }

        logger.info("Testing server connection to " + url);
        try {
            logger.info("Server version: " + nextcloud.getServerVersion());
        }catch (Exception ex){
            logger.log(Level.SEVERE, "Failed to test connection to Nextcloud server", ex);
            return;
        }

        String upload_directory = config.getProperty("upload_directory", "PSNextcloudUSB");
        if (!nextcloud.folderExists(upload_directory)){
            logger.info("Directory " + upload_directory + " does not exist on Nextcloud, creating...");
            nextcloud.createFolder(upload_directory);
        }

        new FileWatcher(
                config.getProperty("root_directory", "/usbdisk.d"),
                upload_directory,
                Integer.parseInt(config.getProperty("check_time_sec", "180")) * 1000,
                Integer.parseInt(config.getProperty("wait_time_sec", "300")) * 1000
        ).run();
    }
}
