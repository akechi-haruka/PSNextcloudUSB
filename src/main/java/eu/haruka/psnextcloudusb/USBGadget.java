package eu.haruka.psnextcloudusb;

import java.io.BufferedReader;
import java.io.IOException;

public class USBGadget {

    public static void remount() throws IOException {
        Main.logger.fine("Remounting");
        Process process = Runtime.getRuntime().exec(new String[]{"bash", Main.ROOT_DIR + "remount.sh"});
        try (BufferedReader ir = process.inputReader()) {
            String line;
            while ((line = ir.readLine()) != null) {
                Main.logger.fine("Remount: " + line);
            }
            while (process.isAlive()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new IOException(e);
                }
            }
        }
    }

}
