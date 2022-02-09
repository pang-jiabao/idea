package com.symedsoft.insurance.vo;

import cn.hutool.core.date.DateUtil;
import com.symedsoft.insurance.config.RequestParamConfig;
import com.symedsoft.insurance.config.SignNoCache;
import com.symedsoft.insurance.utils.DesUtils;
import com.symedsoft.insurance.utils.MapUtils;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/*
 *@author：LL
 *@Date:2021/5/14
 *@Description
 */
@Data
public class CommonRequestVO {
    private String infno;       //交易编号
    private String msgid;       //发送方报文ID
    private String mdtrtarea_admvs;       //就医地医保区划
    private String insuplc_admdvs;       //参保地医保区划
    private String recer_sys_code;       //接收方系统代码
    private String dev_no;       //设备编号
    private String dev_safe_info;       //设备安全信息
    private String cainfo;       //数字签名信息
    private String signtype;       //签名类型
    private String infver;       //接口版本号
    private String opter_type;       //经办人类别
    private String opter;       //经办人
    private String opter_name;       //经办人姓名
    private String inf_time;       //交易时间
    private String fixmedins_code;       //定点医药机构编号
    private String fixmedins_name;       //定点医药机构名称
    private String sign_no;       //交易签到流水号
    private String serv_code;   //服务商识别码：A6B3720544049E1397
    private String serv_sign;   //厂商代码：HH00001
    private Map<String,Object> input;       //入参

    /**
     * 设置当前交易时间
     */
    private Date setDefaultInfTime() {
        Date date = new Date();
        this.inf_time = DateUtil.format(date, "yyyy-MM-dd HH:mm:ss");
        return date;
    }

    /**
     * 设置系统中保存的 sign_no<br/>
     * 没有则为null
     * @Param operateNo
     */
//    private void setDefaultSignNo(String operateNo) {
//        if (SignNoCache.cache.containsKey(operateNo)) {
//            this.sign_no = SignNoCache.cache.get(operateNo);
//        }
//    }

    /**
     * 设置经办人信息
     */
    private void setOpterInfo () {
        this.opter_type = "1";
        this.opter = "001";
        this.opter_name = "管理员";
    }


    /**
     * 设置基础信息
     * @param config
     */
    public void setBaseParams(RequestParamConfig config) {
        this.mdtrtarea_admvs = config.getMdtrtarea_admvs();
        this.infver = config.getInfver();
        this.fixmedins_code = config.getFixmedins_code();
        this.fixmedins_name = config.getFixmedins_name();
        this.opter_type = config.getOperateType();
        this.opter = config.getOperateNo();
        this.opter_name = config.getOperateName();
        this.serv_sign = config.getServ_sign();
        this.serv_code = config.getServ_code();
    }

    /**
     * 构造方法，在new的过程中将一部分参数初始化好<br/>
     * 设置 inf_time、opter、opter_type、opter_name
     */
    public CommonRequestVO() {

        setDefaultInfTime();

        setOpterInfo();
    }

    /**
     * 设置request 请求公共参数默认值<br/>
     * 包含 inf_time、dtrtarea_admvs、infver、<br/>
     * fixmedins_code、fixmedins_name、sign_no、msgid<br/>
     * inf_time、opter、opter_type、opter_name
     * @param config
     * @param seq
     * @Param operateNo
     */
    public CommonRequestVO(RequestParamConfig config , String seq) {

        Date date = setDefaultInfTime();

        setBaseParams(config);

        //setOpterInfo();

        String dateStr = DateUtil.format(date,"yyyyMMddHHmmss");
        this.msgid = config.getFixmedins_code() + dateStr + seq;

    }




}
