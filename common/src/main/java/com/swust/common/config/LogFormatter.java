package com.swust.common.config;

import com.sun.corba.se.impl.activation.ServerMain;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * @author : LiuMingyao
 * @date : 2020/4/11 15:34
 * @description : TODO
 */
public class LogFormatter extends SimpleFormatter {
    private final String lineSeparator = System.getProperty("line.separator");


    public static void init() throws Exception {
        System.setProperty("java.util.logging.config.file", ServerMain.class.getClassLoader().getResource("log.properties").getPath());
        LogManager.getLogManager().readConfiguration();
    }

    @Override
    public synchronized String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        String message = formatMessage(record);
        sb.append(record.getLevel().getLocalizedName());
        sb.append(message);
        sb.append(lineSeparator);
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ignored) {
            }
        }
        return sb.toString();
    }
}