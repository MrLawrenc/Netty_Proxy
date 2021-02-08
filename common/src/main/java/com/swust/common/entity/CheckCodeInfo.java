package com.swust.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件校验码信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckCodeInfo {
    /**
     * adler32弱校验  生成快
     */
    private long weakToken;
    /**
     * md5强校验 生成慢
     */
    private String strongToken;
}