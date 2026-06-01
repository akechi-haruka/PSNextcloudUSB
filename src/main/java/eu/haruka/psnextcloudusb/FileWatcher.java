package eu.haruka.psnextcloudusb;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.stream.Stream;

public class FileWatcher implements Runnable {
    private final String rootDirectory;
    private final String uploadDirectory;
    private final int delay;
    private final ArrayList<String> uploaded = new ArrayList<>();

    public FileWatcher(String root_directory, String upload_directory, int delay) {
        rootDirectory = root_directory;
        uploadDirectory = upload_directory;
        this.delay = delay;
    }

    @Override
    public void run() {
        Main.logger.info("Running...");
        try {
            while (true) {
                Main.logger.fine("Waiting " + delay);
                Thread.sleep(delay);
                USBGadget.remount();

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

                if (System.currentTimeMillis() - lastWrite[0] < delay){
                    Main.logger.fine("File write safety delay not exceeded, waiting...");
                    continue;
                }

                try (Stream<Path> p = Files.find(root,
                        Integer.MAX_VALUE,
                        (_, fileAttr) -> fileAttr.isRegularFile())) {
                    p.forEach((path) -> {
                        Path target = Path.of(uploadDirectory, path.relativize(root).toString());
                        if (!uploaded.contains(target.toString())) {
                            Main.logger.info("Uploading " + path + " to " + target);
                            try {
                                Main.nextcloud.uploadFile(path.toFile(), target.toString());
                                uploaded.add(target.toString());
                            }catch(Exception ex){
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
