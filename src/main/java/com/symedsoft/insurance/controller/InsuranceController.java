package com.symedsoft.insurance.controller;

import com.symedsoft.insurance.common.OpenLog;
import com.symedsoft.insurance.config.SpecialBusinessConfig;
import com.symedsoft.insurance.service.*;
import com.symedsoft.insurance.utils.ReadDll;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/*
 *@author：LL
 *@Date:2021/5/12
 *@Description
 */
@Api("医保接口")
@RestController
@RequestMapping
public class InsuranceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(InsuranceController.class);


    @Autowired
    private InsuranceConfigService insuranceConfigService;

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private DictService dictService;

    @Autowired
    private CheckAcctService checkAcctService;

    @Autowired
    private AutoUploadService autoUploadService;

    @Autowired
    private SignService signService;

    @Autowired
    private CollectionUploadService collectionUploadService;

    @Autowired
    private SpecialBusinessConfig specialBusinessConfig;



    /**
     * 目录下载，
     * @param interfaceCode 接口编号
     * @return 1 成功 ， -1 失败
     */
    @OpenLog
    @PostMapping("/downloadCatalog")
    public String downLoadCatalog(@RequestParam String interfaceCode) {
        String ret = "";
        try{
            Map<String , Object> param = insuranceConfigService.getInputJsonStrByDownload(interfaceCode);

            boolean flag = catalogService.doInsuranceAndParse(param);

            if (flag) {
                ret = catalogService.saveFileIntoTable(param);
            }
        } catch(Exception e) {
            LOGGER.error("目录下载接口：" + e);
            ret = e.getMessage();
            if (ret.length() > 100){
                ret = ret.substring(0 , 99);
            }
        }
        return ret;
    }

    /**
     * 目录查询
     * @param serialNo
     * @param interfaceCode
     * @return
     */
    @OpenLog
    @PostMapping("/queryCatalog")
    public String queryCatalog(@RequestParam String serialNo,
                               @RequestParam String interfaceCode) {
        String jsonStr = "-1";
        try {
            jsonStr = insuranceConfigService.getInputJsonStr(serialNo , interfaceCode, true);
            byte[] outpchar = new byte[1024*64];
            LOGGER.info(interfaceCode +"---"+serialNo +"*************************医保调用开始*************************" );
            LOGGER.info(interfaceCode +"---"+serialNo+"**************入参内容:*********:"+jsonStr);
            int result = ReadDll.INSTANCE.BUSINESS_HANDLE(jsonStr.getBytes("GBK"), outpchar);
            String outpStr = new String(outpchar,"GBK");
            LOGGER.info(interfaceCode +"---"+serialNo+"**************出参内容:*********:"+outpStr.trim());
            LOGGER.info(interfaceCode +"---"+serialNo + "*************************医保调用结束*************************");
            if (result != 0){
                LOGGER.error("interfaceCode为：{},serialNo为:{} 的医保调用失败" , interfaceCode, serialNo);
            }
            jsonStr = insuranceConfigService.saveOutputStr(serialNo, interfaceCode , outpStr , true);
        } catch (Exception e) {
            LOGGER.error("目录查询接口：" + e);
            jsonStr = e.getMessage();
            if (jsonStr.length() > 100){
                jsonStr = jsonStr.substring(0 , 99);
            }
        }
        return jsonStr ;
    }

    /**
     * 调用医保接口
     * @param serialNo 入参配置表id
     * @param interfaceCode 接口编号
     * @param verify 不传参时默认true， true-用入参表的操作人信息（必填） false-操作人信息读取yml配置
     * @return 1 成功 ， -1 失败
     */
    @OpenLog
    @PostMapping("/call")
    public String callInsuranceService(@RequestParam String serialNo,
                                       @RequestParam(required = false,defaultValue = "true") boolean verify,
                                       @RequestParam String interfaceCode,
                                       @RequestParam(required = false,defaultValue = "0") String newborn) {
        String ret = "";
        try{
            byte[] outpchar = new byte[1024*1024];
            //解析入参
            LOGGER.info(interfaceCode + "---" +serialNo + "*************************入参解析开始*************************");
            String inputStr  = insuranceConfigService.getInputJsonStr(serialNo,interfaceCode,verify);
            //调用医保接口
            LOGGER.info(interfaceCode +"---"+serialNo +"*************************医保调用开始*************************" );
            LOGGER.info(interfaceCode +"---"+serialNo+"**************入参内容:*********:"+inputStr);
            int res =  ReadDll.INSTANCE.BUSINESS_HANDLE(inputStr.getBytes("GBK"), outpchar);
            String outputStr = new String(outpchar,"GBK");
            outputStr = outputStr.trim();
            LOGGER.info(interfaceCode +"---"+serialNo+"**************出参内容:*********:"+outputStr);
            LOGGER.info(interfaceCode +"---"+serialNo + "*************************医保调用结束*************************");
            if(res < 0){
                LOGGER.error(interfaceCode +"---"+serialNo +"****************callInsuranceService医保接口调用失败****************");
            }
            //保存出参
            if ("5204".equals(interfaceCode)) {
                ret = insuranceConfigService.saveOutputStr5402(serialNo , interfaceCode , outputStr , false);
            } else {
                if (specialBusinessConfig.getReadCards().contains(interfaceCode)) {
                    ret = insuranceConfigService.saveReadCardOutputStr(serialNo , interfaceCode , outputStr , false, newborn);
                } else {
                    ret = insuranceConfigService.saveOutputStr(serialNo,interfaceCode,outputStr , false);
                }
                //ret = insuranceConfigService.saveOutputStr(serialNo,interfaceCode,outputStr,false);
            }
            LOGGER.info(interfaceCode +"*************************出参保存完成:"+ret+"*************************");
        } catch (Exception e) {
            LOGGER.error("调用医保接口异常——" + interfaceCode + serialNo + ":", e);
            ret = e.getMessage();
            if (ret.length() > 100){
                ret = ret.substring(0 , 99);
            }
        }
        return ret;
    }

    /**
     * 调用医保接口(仅公共参数)
     * @param serialNo 入参配置表id
     * @param interfaceCode 接口编号
     * @return 1 成功 ， -1 失败
     */
    @OpenLog
    @PostMapping("/call2")
    public String callInsuranceServiceWithOutInput(@RequestParam String serialNo,
                                       @RequestParam String interfaceCode) {
        String ret = "";
        try{
            byte[] outpchar = new byte[1024*1024];
            //解析入参
            String inputStr  = insuranceConfigService.getCommonJsonStr(serialNo,interfaceCode);
            //调用医保接口
            //调用医保接口
            LOGGER.info(interfaceCode +"---"+serialNo +"*************************医保调用开始*************************" );
            LOGGER.info(interfaceCode +"---"+serialNo+"**************入参内容:*********:"+inputStr);
            int res =  ReadDll.INSTANCE.BUSINESS_HANDLE(inputStr.getBytes("GBK"), outpchar);
            String outputStr = new String(outpchar,"GBK");
            LOGGER.info(interfaceCode +"---"+serialNo+"**************出参内容:*********:"+outputStr.trim());
            LOGGER.info(interfaceCode +"---"+serialNo + "*************************医保调用结束*************************");
            if(res < 0){
                LOGGER.error(interfaceCode +"---"+serialNo +"****************callInsuranceService医保接口调用失败****************");
            }
            //保存出参
            ret = insuranceConfigService.saveOutputStr(serialNo,interfaceCode,outputStr,false);
        } catch(Exception e) {
            LOGGER.error("调用医保接口异常——" + interfaceCode + serialNo + ":", e);
            ret = e.getMessage();
            if (ret.length() > 100){
                ret = ret.substring(0 , 99);
            }
        }
        return ret;
    }



    /**
     * 获取入参字符串
     * @param serialNo 入参配置表id
     * @param interfaceCode 接口编号
     * @return 1 成功 ， -1 失败
     */
    @OpenLog
    @PostMapping("/getInput")
    public String getInputJsonStr(@RequestParam String serialNo,
                                  @RequestParam(required = false,defaultValue = "true") boolean verify,
                                  @RequestParam String interfaceCode) throws Exception {
        String jsonStr = "-1";
        try{
            jsonStr = insuranceConfigService.getInputJsonStr(serialNo,interfaceCode, verify);
            LOGGER.info(interfaceCode +"---"+serialNo+"拼接入参************:参数:"+jsonStr);
        } catch(Exception e) {
            LOGGER.error("获取入参字符串接口：" + e);
            throw e;
        }
        return jsonStr;
    }

    /**
     * 保存出参字符串
     * @param serialNo 入参配置表id
     * @param interfaceCode 接口编号
     * @param outpStr 医保出参
     * @return 1 成功 ， -1 失败
     */
    @OpenLog
    @PostMapping("/saveOutput")
    public String saveOutputStr(@RequestParam String serialNo,
                                @RequestParam String interfaceCode,
                                @RequestParam String outpStr,
                                @RequestParam(required = false,defaultValue = "0") String newborn) throws Exception {
        String ret = "";
        try{
            LOGGER.info(interfaceCode +"---"+serialNo +"保存出参************:参数:"+outpStr);
            if (specialBusinessConfig.getReadCards().contains(interfaceCode)) {
                ret = insuranceConfigService.saveReadCardOutputStr(serialNo , interfaceCode , outpStr , false , newborn);
            } else {
                ret = insuranceConfigService.saveOutputStr(serialNo,interfaceCode,outpStr , false);
                //调用结算接口时如果保存数据失败 调用冲正方法冲正接口
                if ("2207".equals(interfaceCode)||"2304".equals(interfaceCode)){
                    if ("-1".equals(ret)){
                        LOGGER.info("调用冲正接口开始：" + interfaceCode+"冲正序号:"+serialNo);
                        String r="";
                        r = insuranceConfigService.call2601(serialNo, interfaceCode);

                        if ("1".equals(r)){
                            if ("2207".equals(interfaceCode)){
                                insuranceConfigService.updateOutpSetterInFlag(serialNo);
                                insuranceConfigService.updateOutpSetterOutFlag(serialNo);
                            }else {
                                insuranceConfigService.updateInpSetterInFlag(serialNo);
                                insuranceConfigService.updateInpSetterOutFlag(serialNo);
                            }
                            LOGGER.info("调用冲正接口成功：" + interfaceCode+"冲正序号:"+serialNo);
                        }else{
                            LOGGER.error("调用冲正接口失败：" + interfaceCode+"冲正序号:"+serialNo);
                        }
                    }
                }
            }
        }catch(Exception e){
            LOGGER.error("保存出参字符串接口：" + e);
            if ("2207".equals(interfaceCode)||"2304".equals(interfaceCode)) {
                LOGGER.info("调用冲正接口开始：" + interfaceCode + "冲正序号:" + serialNo);
                String r = "";
                //调用结算接口时如果保存数据失败 调用冲正方法冲正接口
                r = insuranceConfigService.call2601(serialNo, interfaceCode);

                if ("1".equals(r)) {
                    if ("2207".equals(interfaceCode)) {
                        insuranceConfigService.updateOutpSetterInFlag(serialNo);
                        insuranceConfigService.updateOutpSetterOutFlag(serialNo);
                    } else {
                        insuranceConfigService.updateInpSetterInFlag(serialNo);
                        insuranceConfigService.updateInpSetterOutFlag(serialNo);
                    }
                    LOGGER.info("调用冲正接口成功：" + interfaceCode + "冲正序号:" + serialNo);
                } else {
                    LOGGER.error("调用冲正接口失败：" + interfaceCode + "冲正序号:" + serialNo);
                }
            }
            ret = e.getMessage();
            if (ret.length() > 100){
                ret = ret.substring(0 , 99);
            }
        }
        return ret;
    }

    @OpenLog
    @PostMapping("/refreshDict")
    public String refreshDict () {
        String ret = "";
        try {
            boolean flag = dictService.refresh();
            ret = flag ? "1" : "-1";
        } catch (Exception e) {
            LOGGER.error("字典接口：字典刷新异常：" + e);
            ret = e.getMessage();
            if (ret.length() > 100){
                ret = ret.substring(0 , 99);
            }
        }
        return ret;
    }
    @OpenLog
    @PostMapping("/checkAcct")
    public String checkAcct(@RequestParam String serialNo){
        String flag = "";
        try {
            flag = checkAcctService.doCheckAcctDetail(serialNo);
        } catch (Exception e) {
            LOGGER.error("详细对账接口发生异常：{}" , e);
        }
        return flag ;
    }

//        @OpenLog
    @RequestMapping("/getMac")
    public String getMac (HttpServletRequest request) {
      //return getMacAddressService.getMacAddress(request);
        char [] a=null;
        String spear= File.separator;
        System.out.println(System.getProperty("user.dir")+spear+"insur"+spear+"SiInterface_hsf");
        int init = ReadDll.INSTANCE.INIT(a);
        return "";
    }

    @OpenLog
    @PostMapping("/getInp1162")
    public String getInp1162(@RequestParam String serialNo, @RequestParam(required = false,defaultValue = "true") boolean verify,
                             @RequestParam String interfaceCode){
        String json = "";
        try {
            json = insuranceConfigService.getInp1162(serialNo,interfaceCode);
            LOGGER.info("1162"+"---"+serialNo+"**************入参内容:*********:"+json);
        } catch (Exception e) {
            LOGGER.error("详细对账接口发生异常：{}" , e);
        }
        return json ;
    }

    @PostMapping("/uploadFee")
    public String uploadFee(@RequestParam String patientId, @RequestParam int visitId){
        String json ;
        try {
            json = autoUploadService.uploadinpFee(patientId,visitId);
        } catch (Exception e) {
            LOGGER.error("公共入参拼接异常：{}" , e);
            json = e.getMessage();
            if (json.length() > 100){
                json = json.substring(0 , 99);
            }
        }
        LOGGER.info("自动上传费用controller，返回pb信息为：{}", json);
        return json ;
    }

    @OpenLog
    @PostMapping("/getCommonJsonStr")
    public String getCommonJsonStr(@RequestParam String serialNo, @RequestParam String interfaceCode){
        String json = "";
        try {
            json = insuranceConfigService.getCommonJsonStr(serialNo,interfaceCode);
        } catch (Exception e) {
            LOGGER.error("公共入参拼接异常：{}" , e);
        }
        return json ;
    }

    @OpenLog
    @PostMapping("/signOut")
    public String signOut(@RequestParam String operateNo) throws UnsupportedEncodingException {
        String json = "";
        try {
            json = signService.signOut(operateNo);
        } catch (Exception e) {
            LOGGER.error("公共入参拼接异常：{}" , e);
        }
        return json ;
    }

    @OpenLog
    @PostMapping("/collectionUpload")
    public String collectionUpload(String interfaceCode) {
        String ret = "1";
        try {
            String param = collectionUploadService.getParamByCode(interfaceCode);
            collectionUploadService.runTask(param);
        } catch (Exception e) {
            LOGGER.error("采集上传：{}失败，失败原因：{}" , interfaceCode , e);
            ret = e.getMessage();
            if (ret.length() > 100){
                ret = ret.substring(0 , 99);
            }
        }
        return ret;
    }

    @OpenLog
    @PostMapping("/call2601")
    public String call2601(@RequestParam String serialNo,
                           @RequestParam String interfaceCode) {
        String ret = "1";
        try {
            ret = insuranceConfigService.call2601(serialNo, interfaceCode);
        }catch (Exception e){
            return "-1";
        }
        return ret;
    }
}
