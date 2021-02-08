package com.swust.client;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

/**
 * @author : MrLawrenc
 * date  2020/10/5 10:51
 * rsync增量同步
 * 同步服务端文件使用zsync（美团也在用，服务器只需要计算一次sign，然后各客户端自行下载sign进行比对同步）
 * 文件处理器。 滑动块检测
 * <pre>
 *     分块
 *     弱摘要采用Adler-32，生成速度快，但是可能出现重复。
 *     强摘要采用md5，生成慢，但是有保障。
 * </pre>
 */
@Slf4j
public final class FileUtil {
    private final static String hexStr = "0123456789ABCDEF";
    private final static String[] binaryArray =
            {"0000", "0001", "0010", "0011",
                    "0100", "0101", "0110", "0111",
                    "1000", "1001", "1010", "1011",
                    "1100", "1101", "1110", "1111"};

    public static void main(String[] args) {
        File file = new File("E:/test1.txt");
        log.info("原始文件大小 : {} B", file.length());
        //单位 kb
        FileInfo fileInfo = blockFile(file, 1024 * 512);
        log.info(JSON.toJSONString(fileInfo));
        log.info("文件信息大小 : {} B", JSON.toJSONString(fileInfo).length());

    }

    public static FileInfo blockFile(File file, int byteSize) {
        try (InputStream inputStream = new FileInputStream(file)) {
            return blockFile0(inputStream, byteSize);
        } catch (Exception e) {
            throw new RuntimeException("文件分块异常", e);
        }
    }

    public static FileInfo blockFile(byte[] data, int byteSize) {
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            return blockFile0(inputStream, byteSize);
        } catch (Exception e) {
            throw new RuntimeException("文件分块异常", e);
        }
    }

    /**
     * 文件分块，每个块大小为byteSize，最后一个块一定小于等于byteSize
     */
    public static FileInfo blockFile0(InputStream inputStream, int byteSize) {

        FileInfo fileInfo = new FileInfo(byteSize);
        List<CheckCodeInfo> checkCodeInfos = new ArrayList<>();
        fileInfo.setCheckCodeInfos(checkCodeInfos);

        byte[] data = new byte[byteSize];
        int readSize;
        try {
            while ((readSize = inputStream.read(data)) != -1) {

                //last block
                if (readSize != fileInfo.getEveryEvenlyBlockSize()) {
                    data = Arrays.copyOfRange(data, 0, readSize);
                    checkCodeInfos.add(new CheckCodeInfo(adler32(data), calculationMd5(data)));
                    log.debug("last file block done,size : {} B", readSize);
                    break;
                }
                log.debug("file block done,size : {} B", readSize);
                //使用adler32弱校验和md5强校验配合
                CheckCodeInfo checkCodeInfo = new CheckCodeInfo(adler32(data), calculationMd5(data));
                checkCodeInfos.add(checkCodeInfo);
            }
            return fileInfo.setLastBlockSize(readSize);
        } catch (Exception e) {
            throw new RuntimeException("文件信息读取异常", e);
        }
    }


    public static String calculationMd5(byte[] data) {
        try {
            //获取 MessageDigest 对象，参数为 MD5 字符串，表示这是一个 MD5 算法（其他还有 SHA1 算法等）：
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            //update(byte[])方法，输入原数据
            //类似StringBuilder对象的append()方法，追加模式，属于一个累计更改的过程
            md5.update(data);
            //digest()被调用后,MessageDigest对象就被重置，即不能连续再次调用该方法计算原数据的MD5值。可以手动调用reset()方法重置输入源。
            //digest()返回值16位长度的哈希值，由byte[]承接
            byte[] md5Array = md5.digest();
            //byte[]通常我们会转化为十六进制的32位长度的字符串来使用,本类包含三种常用的转换方法
            return bytesToHex3(md5Array);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    public static String bytesToHex3(byte[] byteArray) {
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] resultCharArray = new char[byteArray.length * 2];
        int index = 0;
        for (byte b : byteArray) {
            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
            resultCharArray[index++] = hexDigits[b & 0xf];
        }
        return new String(resultCharArray);
    }

    /**
     * @param hexString
     * @return 将十六进制转换为字节数组
     */
    public static byte[] hexStringToBinary(String hexString) {
        //hexString的长度对2取整，作为bytes的长度
        int len = hexString.length() / 2;
        byte[] bytes = new byte[len];
        byte high = 0;//字节高四位
        byte low = 0;//字节低四位

        for (int i = 0; i < len; i++) {
            //右移四位得到高位
            high = (byte) ((hexStr.indexOf(hexString.charAt(2 * i))) << 4);
            low = (byte) hexStr.indexOf(hexString.charAt(2 * i + 1));
            bytes[i] = (byte) (high | low);//高地位做或运算
        }
        return bytes;
    }

    /**
     * @return 转换为二进制字符串
     */
    public static String bytes2BinaryStr(byte[] bArray) {

        StringBuilder outStr = new StringBuilder();
        int pos;
        for (byte b : bArray) {
            //高四位
            pos = (b & 0xF0) >> 4;
            outStr.append(binaryArray[pos]);
            //低四位
            pos = b & 0x0F;
            outStr.append(binaryArray[pos]);
        }
        return outStr.toString();

    }

    /**
     * Java Byte之Adler32算法对数据流的校验
     * <p>
     * 可用于计算数据流的 Adler-32 校验和的类。Adler-32 校验和几乎与
     * CRC-32 一样可靠，但是能够更快地计算出来。
     */
    public static long adler32(byte[] data) {
        Checksum checksumEngine = new Adler32();
        checksumEngine.update(data, 0, data.length);
        return checksumEngine.getValue();
    }

    @Data
    @Accessors(chain = true)
    public static class FileInfo {
        private String filePath;
        /**
         * 数据块的校验码信息，包含 弱滚动校验码和强校验码   最后一个为末尾的数据块校验码信息
         */
        private List<CheckCodeInfo> checkCodeInfos;
        /**
         * 每个均匀数据块的大小 单位是kb
         */
        private final int everyEvenlyBlockSize;
        /**
         * 最后一个数据块的大小，一定满足 {@link FileInfo#lastBlockSize}<={@link FileInfo#everyEvenlyBlockSize}
         */
        private int lastBlockSize;

        public FileInfo(int everyEvenlyBlockSize) {
            this.everyEvenlyBlockSize = everyEvenlyBlockSize;
        }
    }


    /**
     * 校验码信息
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CheckCodeInfo {
        /**
         * adler32弱校验  生成快
         */
        private long weakToken;
        /**
         * md5强校验 生成慢
         */
        private String strongToken;
    }

}