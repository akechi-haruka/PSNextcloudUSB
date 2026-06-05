package eu.haruka.psnextcloudusb;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.stream.Stream;

public class FileWatcher implements Runnable {
    private final String rootDirectory;
    private final String uploadDirectory;
    private final int checkDelay;
    private final int writeDelay;
    private final ArrayList<String> uploaded = new ArrayList<>();

    public FileWatcher(String root_directory, String upload_directory, int check_delay, int write_delay) {
        rootDirectory = root_directory;
        uploadDirectory = upload_directory;
        checkDelay = check_delay;
        writeDelay = write_delay;
    }

    @Override
    public void run() {
        Main.logger.info("Running...");
        Main.logger.info("Check Delay: " + checkDelay);
        Main.logger.info("Write Delay: " + writeDelay);
        try {
            while (true) {
                Main.logger.fine("Waiting " + checkDelay);
                Thread.sleep(checkDelay);

                final long[] lastWrite = {0};
                Path root = Paths.get(rootDirectory);
                try (Stream<Path> p = Files.find(root,
                        Integer.MAX_VALUE,
                        (_, fileAttr) -> fileAttr.isRegularFile())) {
                    p.forEach((path) -> {
                        lastWrite[0] = Math.max(lastWrite[0], path.toFile().lastModified());
                    });
                }

                Main.logger.fine("Last file write: " + lastWrite[0]);

                if (System.currentTimeMillis() - lastWrite[0] < writeDelay) {
                    Main.logger.fine("File write safety delay not exceeded (" + (System.currentTimeMillis() - lastWrite[0]) + "), waiting...");
                    continue;
                }

                try (Stream<Path> p = Files.find(root,
                        Integer.MAX_VALUE,
                        (_, fileAttr) -> fileAttr.isRegularFile())) {
                    p.forEach((path) -> {
                        Path target = Path.of(uploadDirectory, root.relativize(path).toString());
                        String targetName = target.toString();
                        if (!uploaded.contains(targetName) && !targetName.contains("System Volume Information")) {
                            Main.logger.info("Uploading " + path + " to " + targetName);
                            try {
                                Path parent = target.getParent();
                                if (!Main.nextcloud.folderExists(parent.toString())){
                                    Main.logger.info("Creating directory: " + parent);
                                    Main.nextcloud.createFolder(parent.toString());
                                }
                                Main.nextcloud.uploadFile(path.toFile(), targetName);
                                uploaded.add(targetName);
                            } catch (Exception ex) {
                                Main.logger.log(Level.WARNING, "Failed to upload " + path + " to " + target + ", will retry next run!", ex);
                            }
                        } else {
                            Main.logger.fine(target + " was already uploaded");
                        }
                    });
                }
            }
        } catch (Throwable tr) {
            Main.logger.log(Level.SEVERE, "Fatal program error", tr);
        }
    }
}
