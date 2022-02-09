package com.symedsoft.insurance.service;

public interface CheckAcctService {

    /**
     * 【3202】医药机构费用结算对明细账<br/>
     * 1、根据 serialNo 查询 入参表信息，取对账时间<br/>
     * 2、根据对账时间取待上传数据<br/>
     * 3、对待上传数据进行校验<br/>
     * 4、生成数据文件并压缩<br/>
     * 5、将文件转换成base64，并调用上传接口上传该文件<br/>
     * 6、解析出参，并将 file_qury_no 加入入参，并调用3202接口<br/>
     * 7、解析出参，获取需要下载的文件的file_qury_no，并调用下载<br/>
     * 8、将文件写入表中<br/>
     * @param serialNo
     * @throws Exception
     */
    String doCheckAcctDetail(String serialNo) throws Exception;
}
