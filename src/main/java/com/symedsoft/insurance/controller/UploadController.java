package com.symedsoft.insurance.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.symedsoft.insurance.service.UploadService;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;


@Api("医保接口")
@RestController
@RequestMapping
public class UploadController {
    @Resource
    private UploadService uploadService;

    @PostMapping("/AutoUpload4101")
    public String autoUpload4101(){
        String json ="";
        try {
            uploadService.autoUpload4101();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json ;
    }

    @PostMapping("/upload")
    public String autoUpload(String interfaceCode){
        String json ="";
        try {
            json = uploadService.callInterfaceBySerialNo(interfaceCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json ;
    }
    @PostMapping("/uploadById")
    public String autoUploadById(String interfaceCode){
        String json ="";
        try {
            json = uploadService.callInterfaceById(interfaceCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json ;
    }

    @PostMapping("/autoUploadByIdNum")
    public String autoUploadByIdNum(String interfaceCode,String id){
        String json ="";
        try {
            json = uploadService.callInterfaceById(interfaceCode,id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json ;
    }
    @PostMapping("/callProcedure")
    public String callProcedure(String procedureName){
        String json ="";
        try {
            json=uploadService.callProcedure(procedureName).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json ;
    }

    @PostMapping("/detailAuditBefore")
    public String testDetailAuditBefore(String serialNo,String trigScen){
        String json ="";
        try {
            json=uploadService.detailAuditBefore( serialNo,trigScen);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json ;
    }
    @PostMapping("/detailAuditAfter")
    public String testDetailAuditAfter(String serialNo,String trigScen){
        String json ="";
        try {
            json=uploadService.detailAuditAfter(serialNo,trigScen);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json ;
    }

    @PostMapping("/saveAuditBefore")
    public String saveDetailAuditBefore(String json,String serialNo){
        JSONObject jsonObject = JSONObject.parseObject(json);
        JSONObject output=jsonObject.getJSONObject("output");
        JSONArray result=output.getJSONArray("result");
        for(int i=0;i<result.size();i++){
            String ret=result.get(i).toString();
            Map mapObj = JSONObject.parseObject(ret,Map.class);
            mapObj.put("serial_no",serialNo);
            mapObj.put("patient_id","M0044394");
            mapObj.put("operate_no","846");
            uploadService.insertDetailAuditBfResultOut(mapObj);
            JSONArray judge_result_detail_dtos=result.getJSONObject(i).getJSONArray("judge_result_detail_dtos");
            for (int j=0;j<judge_result_detail_dtos.size();j++){
                String detail=judge_result_detail_dtos.get(j).toString();
                Map detailMap=JSONObject.parseObject(detail,Map.class);
                detailMap.put("serial_no",serialNo);
                detailMap.put("patient_id","M0044394");
                detailMap.put("operate_no","846");
                uploadService.insertDetailAuditBfDetailOut(detailMap);
            }
        }
        return "1";
    }

    @PostMapping("/saveAuditAfter")
    public String saveAuditAfter(String json,String serialNo){
        JSONObject jsonObject = JSONObject.parseObject(json);
        JSONObject output=jsonObject.getJSONObject("output");
        JSONArray result=output.getJSONArray("result");
        for(int i=0;i<result.size();i++){
            String ret=result.get(i).toString();
            Map mapObj = JSONObject.parseObject(ret,Map.class);
            mapObj.put("serial_no",serialNo);
            mapObj.put("patient_id","M0044394");
            mapObj.put("operate_no","846");
            uploadService.insertDetailAuditAfterResultOut(mapObj);
            JSONArray judge_result_detail_dtos=result.getJSONObject(i).getJSONArray("judge_result_detail_dtos");
            for (int j=0;j<judge_result_detail_dtos.size();j++){
                String detail=judge_result_detail_dtos.get(j).toString();
                Map detailMap=JSONObject.parseObject(detail,Map.class);
                detailMap.put("serial_no",serialNo);
                detailMap.put("patient_id","M0044394");
                detailMap.put("operate_no","846");
                uploadService.insertDetailAuditAfterDetailOut(detailMap);
            }
        }
        return "1";
    }
}
