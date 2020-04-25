package com.swust.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author : MrLawrenc
 * @date : 2020/4/25 16:51
 * @description : 服务端参数
 */
@Data@AllArgsConstructor
public class ServerConfig {
    private String serverPassword;
    private String serverPort;
}