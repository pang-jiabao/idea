package com.symedsoft.insurance;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.symedsoft.insurance.mapper.ParticipateMapper;
import com.symedsoft.insurance.service.*;
import com.symedsoft.insurance.utils.ReadDll;
import com.symedsoft.insurance.vo.CommonResponseVO;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = InsuranceApplication.class)
@TestPropertySource("classpath:application-dev.yml")
//测试环境使用，用来表示测试环境使用的ApplicationContext将是WebApplicationContext类型的
@WebAppConfiguration
//@AutoConfigureMockMvc是用于自动配置MockMvc
@AutoConfigureMockMvc
public class InsuranceApplicationTests {

    Logger LOGGER = LoggerFactory.getLogger(InsuranceApplicationTests.class);

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private AutoUploadService autoUploadService;
    @Autowired
    private SignService signService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CheckAcctService checkAcctService;
    @Autowired
    private ParticipateMapper participateMapper;
    @Autowired
    private UploadService uploadService;

    @Autowired
    private CollectionUploadService collectionUploadService;
    private String serialNo;
    private String interfaceCode;
    private String outpStr;
    private String url;


    @Before()
    public void setup(){
        mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();//建议使用这种
    }

    private void mockConfig() throws Exception {
        char [] c=new char[100];
        ReadDll.INSTANCE.INIT(c);
        //post传参JSON
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(url)
                //.content(jsonObject.toJSONString())
                .param("serialNo",serialNo)
                .param("interfaceCode",interfaceCode)
                .param("outpStr",outpStr)
                .contentType(MediaType.APPLICATION_JSON));
        //乱码
        MockHttpServletResponse response = resultActions.andReturn().getResponse();
        response.setCharacterEncoding("UTF-8");
        //验证、打印
        resultActions.andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(MockMvcResultHandlers.print())
        .andReturn();
        LOGGER.info("*****************result:{}"+response.getContentAsString());
    }

    /**
     * 获取入参
     * @throws Exception
     */
    @Test
    public void getInputJsonStr() throws Exception {
        url ="/call";
        interfaceCode = "1101";
        serialNo = "2021061116424709";
        mockConfig();
    }

    /**
     * 获取入参
     * @throws Exception
     */
    @Test
    public void downLoadCatalog() throws Exception {
//        url ="/downloadCatalog";
//        interfaceCode = "1301";
        //serialNo = "20210518144622040001";
        mockConfig();
    }

    /**
     * 获取入参
     * @throws Exception
     */
    @Test
    public void getcommonInput() throws Exception {
        url ="/call2";
        interfaceCode = "1301";
        //serialNo = "20210518144622040001";
        mockConfig();
    }

    /**
     * 保存出参
     * @throws Exception
     */
    @Test
    public void saveOutputStr() throws Exception {
        url ="/saveOutput";
        interfaceCode = "2201";
        serialNo = "2021051401445";
        CommonResponseVO response  = new CommonResponseVO();
        response.setInfCode("0");
        response.setInf_refmsgid("0000002021051513451010101010");
        response.setRefmsg_time("202105151345212");
        response.setRespond_time("202105151345785");
        response.setErr_msg("");

        Map<String, Object> data =  new HashMap<>();
        data.put("mdtrt_id", "M8788");
        data.put("psn_no", "8187457");
        data.put("ipt_otp_no","10" );

        Map<String, Object> outMap = new HashMap<>();
        outMap.put("data", data);
        response.setOutput(outMap);
        outpStr= JSONObject.toJSONString(response);
        mockConfig();
    }

    @Test
    public void autoUpload() throws Exception {
        BigDecimal oldFee=new BigDecimal(0);
        BigDecimal a=new BigDecimal(-2.2);
        BigDecimal add = oldFee.add(a);
        System.out.println(add);
        //autoUploadService.uploadInpFee();
//        String patientId="000000054909";
//        System.out.println(patientId.replace("000000054",""));//patientId.substring(patientId.indexOf("000000054")));
    }

    @Test
    public void sign() throws Exception {

//        char [] c=new char[100];
//        ReadDll.INSTANCE.INIT(c);
//        signService.signIn("2825");
        //uploadService.autoUpload4401();
//        uploadService.callInterface("4402");
//        uploadService.callProcedure("insursyjk.detail_audit");
    }

    @Test
    public void uploadTask() {
        char [] c=new char[100];
        ReadDll.INSTANCE.INIT(c);
        collectionUploadService.runTask("INSUR_RCPT_ITEM_UP_SETLINFO_IN,4101");
    }
    @Test
    @SneakyThrows
    public void checkAcct() {

    }
    @Test
    public void ttt() {
        Date date = new Date();
        System.out.println(date);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        System.out.println(simpleDateFormat.format(date));
        String data = " {\"id\":\"423\",\"serial_no\":1000,\"patient_id\":\"2000\"," +
                "\"operate_no\":\"3000\",\"visit_date\":\""+simpleDateFormat.format(date)+"\",\"psn_no\":\"5000\",\"rpotc_no\":\"6000\"," +
                "\"fixmedins_code\":\"10001\"}";
        Map<String,Object> map = JSON.parseObject(data);
        String tableNum="RESULTDETS_RPTDETAILINFO_IN";

        Map<String,Object> columnMap = new HashMap<>();
        columnMap.put("tableNum",tableNum);
        columnMap.put("columnMap",map);
        com.symedsoft.insurance.controller.Test t = new com.symedsoft.insurance.controller.Test();

        System.out.println( "sql执行返回值:"+ participateMapper.insertTB1(columnMap));
    }
}
