package com.onlinebookstore.entity.userserver;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author rkc
 * @version 1.0
 * @date 2020/9/11 9:35
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Address implements Serializable {

    private static final long serialVersionUID = -6473025163554349305L;
    /**
     * 主键自增长字段
     */
    private Integer id;

    /**
     * 账号表的外键字段
     */
    @JsonProperty("account_username")
    private String accountUsername;

    /**
     * 收货电话号码
     */
    private String phone;

    /**
     * 签收人姓名
     */
    @JsonProperty("receiver_name")
    private String receiverName;

    /**
     * 地址
     */
    private String address;

    /**
     * 账户对象
     */
    private Account account;
}
