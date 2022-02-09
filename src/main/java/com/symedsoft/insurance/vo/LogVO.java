package com.symedsoft.insurance.vo;

import java.util.Date;

public class LogVO {
    private String serialNo;
    private String patientId;
    private Date visitDate;
    private String visitId;
    private String operateNo;
    private String interfaceCode;
    private String inputJson;
    private String msgid;
    private String exportJson;
    private String infCode;
    private String inf_refmsgid;
    private String refmsg_time;
    private String respond_time;
    private String err_msg;

    public String getSerialNo() {
        return serialNo;
    }

    public String getPatientId() {
        return patientId;
    }

    public Date getVisitDate() {
        return visitDate;
    }

    public String getVisitId() {
        return visitId;
    }

    public String getOperateNo() {
        return operateNo;
    }

    public String getInterfaceCode() {
        return interfaceCode;
    }

    public String getInputJson() {
        return inputJson;
    }

    public String getExportJson() {
        return exportJson;
    }

    public String getInfCode() {
        return infCode;
    }

    public String getInf_refmsgid() {
        return inf_refmsgid;
    }

    public String getRefmsg_time() {
        return refmsg_time;
    }

    public String getRespond_time() {
        return respond_time;
    }

    public String getErr_msg() {
        return err_msg;
    }

    public String getMsgid() {
        return msgid;
    }

    public void setMsgid(String msgid) {
        this.msgid = msgid;
    }

    public LogVO setSerialNo(String serialNo) {
        this.serialNo = serialNo;
        return this;
    }

    public LogVO setPatientId(String patientId) {
        this.patientId = patientId;
        return this;
    }

    public LogVO setVisitDate(Date visitDate) {
        this.visitDate = visitDate;
        return this;
    }

    public LogVO setVisitId(String visitId) {
        this.visitId = visitId;
        return this;
    }

    public LogVO setOperateNo(String operateNo) {
        this.operateNo = operateNo;
        return this;
    }

    public LogVO setInterfaceCode(String interfaceCode) {
        this.interfaceCode = interfaceCode;
        return this;
    }

    public LogVO setInputJson(String inputJson) {
        this.inputJson = inputJson;
        return this;
    }

    public LogVO setExportJson(String exportJson) {
        this.exportJson = exportJson;
        return this;
    }

    public LogVO setInfCode(String infCode) {
        this.infCode = infCode;
        return this;
    }

    public LogVO setInf_refmsgid(String inf_refmsgid) {
        this.inf_refmsgid = inf_refmsgid;
        return this;
    }

    public LogVO setRefmsg_time(String refmsg_time) {
        this.refmsg_time = refmsg_time;
        return this;
    }

    public LogVO setRespond_time(String respond_time) {
        this.respond_time = respond_time;
        return this;
    }

    public LogVO setErr_msg(String err_msg) {
        this.err_msg = err_msg;
        return this;
    }
}
