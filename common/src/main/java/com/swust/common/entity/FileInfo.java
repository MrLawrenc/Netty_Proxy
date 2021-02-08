package com.swust.common.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 文件块信息
 * date   2021/2/8 13:56
 */
@Data
@Accessors(chain = true)
public class FileInfo {
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
