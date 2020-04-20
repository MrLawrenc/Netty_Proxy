package com.swust.common.config;

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
/*        ClassLoader classLoader = LogFormatter.class.getClassLoader();
        URL resource = classLoader.getResource("log.xml");
        System.out.println(resource.getPath());

        InputStream in = LogFormatter.class.getResourceAsStream("/log.xml");
        Properties p = new Properties();
        p.load(in);*/

    /*    File file = new File("");
        System.out.println(file.getAbsolutePath());*/

        //打为jar包之后读取不到配置
        try {
            System.setProperty("java.util.logging.config.file", LogFormatter.class.getResource("log.xml").getPath());
            LogManager.getLogManager().readConfiguration();
        } catch (Exception ignore) {
        }
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