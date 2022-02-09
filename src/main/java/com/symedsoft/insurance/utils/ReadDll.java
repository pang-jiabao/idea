package com.symedsoft.insurance.utils;



import com.sun.jna.Library;
import com.sun.jna.Native;

import java.io.File;

/**
 * 调用dll
 */
public interface ReadDll extends Library {
    String spear= File.separator;
    // javaCallCpp.dll
    // 放到工程目录下才行（与.classpath一级），原文注释写到也可以放到C:\WINDOWS\system32下，但我放到system32下运行项目，依然会报错
//    ReadDll INSTANCE = Native.loadLibrary(
//            System.getProperty("user.dir")+spear+"insur"+spear+"SiInterface_hsf", ReadDll.class);
    String dll="C:\\insur\\SiInterface_hsf.dll";
    ReadDll INSTANCE = (ReadDll) Native.loadLibrary(dll, ReadDll.class);
    // long WINAPI ICC_Reader_Open(char* dev_Name);
    // 打开指定的电脑接口.
    public String ICC_Reader_Open(String data);// 用到

    //调用医保业务
    int BUSINESS_HANDLE(byte[] inputData, byte[] outputData);// 用到

    //初始化函数:
    int INIT(char[]  pErrMsg);
    //从EEprom读取数据
    public int ICC_Reader_ReadEEPROM(int ReaderHandle, int offset, int length, byte[] data);



    public static void main(String[] args) {
//        String json="{\"infno\": \"1162\",\"msgid\": \"H00000000001202001041235391234\",\"insuplc_admdvs\":\"100000\",\"mdtrtarea_admvs\":\"100000\",\"recer_sys_code\":\"MBS_LOCAL\",\"dev_no\":\"\",\"dev_safe_info\":\"\",\"cainfo\": \"\",\"infver\": \"V1.0\",\"opter_type\": \"1\",\"opter\": \"01\",\"opter_name\": \"张三\",\"inf_time\": \"2020-01-04 12:35:39\",\"fixmedins_code\": \"100001\",\"fixmedins_name\": \"第一人民医院\",\"sign_no\": \"79faf82271944fe38c4f1d99be71bc\",\"input\": {\"data\": {\"orgId\": \"100001\",\"businessType\": \"01101\",\"operatorId\":\"111111\",\"operatorName\": \"李四\",\"officeId\": \"101\",\"officeName\": \"内科\"}}}";
//        String hReader = ReadDll.INSTANCE.ICC_Reader_Open("USB1");
//        if ("1".equals(hReader)){
//            System.out.println("调用失败");
//        }
        //System.out.println(System.getProperty("user.dir")+spear+"insur"+spear+"SiInterface_hsf");
        char arr[]=new char[100];
        int init = ReadDll.INSTANCE.INIT(arr);
        System.out.println(init);
        System.out.println(arr);

    }
}
