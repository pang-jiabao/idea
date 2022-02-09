package com.symedsoft.insurance.vo;

import lombok.Data;

/**
 * 配置信息
 */
@Data
public class InsurBusinessConfigVO {
    private String businessid;

    private String io_flag;

    private String config_item;

    private String config_type;

    private String node;

    private short item_no;

    private String relat_bid;

    private String node_table;

    private short multi_line;

    private String relat_node;

    private short node_sort;

    private String node_sql;

    private String node_date_type;

    private String node_time_type;

    private String node_number_type;

}