package buildcraft.core.lib.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;

import buildcraft.api.core.BCLog;

public abstract class StreamConfigManager {
    @SuppressWarnings("serial")
    private final Properties properties = new Properties() {
        @Override
        public synchronized Enumeration<Object> keys() {
            // A way of sorting the properties.
            return Collections.enumeration(new TreeSet<>(super.keySet()));
        }
    };

    /** @param option The option to refresh
     * @return True if something changed as a result of the refresh. */
    protected final boolean refresh(DetailedConfigOption option, String key) {
        if (properties.getProperty(key) == null) read();
        if (properties.getProperty(key) == null) {
            properties.setProperty(key, option.defaultValue());
            option.cache = option.defaultValue();
            readAndWriteFile();
            return true;
        }
        String val = properties.getProperty(key, option.defaultValue());
        if (val.equals(option.cache)) return false;
        option.cache = val;
        readAndWriteFile();
        return true;
    }

    protected void readAndWriteFile() {
        // Just refresh whatever was in it
        read();
        write();
    }

    protected abstract void read();

    protected final void read(InputStream streamIn) {
        if (streamIn != null) {
            try (Reader reader = new InputStreamReader(streamIn)) {
                properties.load(reader);
                reader.close();
            } catch (IOException e) {
                BCLog.logger.warn("Caught an IOException while reading the detailed config file: " + e.getMessage());
            }
        }
    }

    protected abstract void write();

    protected final void write(OutputStream streamOut) {
        if (streamOut == null) return;
        try (Writer writer = new OutputStreamWriter(streamOut)) {
            properties.store(writer, comment());
            writer.close();
        } catch (IOException e) {
            BCLog.logger.warn("Caught an IOException while writing the detailed config file: " + e.getMessage());
        }
    }

    protected abstract String comment();
}
