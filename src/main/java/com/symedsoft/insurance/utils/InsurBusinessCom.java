package com.symedsoft.insurance.utils;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;


public class InsurBusinessCom {
    public  static void main(String[] args){
        String input="{\"tran_no\":\"1101\",\"tran_time\":\"20210602122345\"," +
                "\"data\":{\"fixmedins_code\":\"P51030200074\",\"psn_no\":\"\"," +
                "\"mdtrtarea_admvs\":\"512000\",\"local_type\":\"1\",\"out_type\":\"1\"}}";


       /* ActiveXComponent  connect = new ActiveXComponent("YinHai.CHS.InterfaceSCS");
        System.out.println("-------------");
        Dispatch dis = connect.getObject();
        Long aint_appcode = 55L;
        String astr_appmsg = new String();
        Integer str = -2;
        Variant v1 = new Variant(str,true);//输出参数定义，必须这样，否则得不到输出参数的值
        Variant v2=new Variant();//返回值定义
        v2 = Dispatch.call(dis, "yh_CHS_init",aint_appcode,aint_appcode);
        String input="{\"tran_no\":\"1101\",\"tran_time\":\"20210602122345\"," +
                "\"data\":{\"fixmedins_code\":\"P51030200074\",\"psn_no\":\"\"," +
                "\"mdtrtarea_admvs\":\"512000\",\"local_type\":\"1\",\"out_type\":\"1\"}}";
        Dispatch.call(dis, "yh_CHS_call","1101",input,astr_appmsg);
        System.out.println(v2);*/
    }
    public static String call(Long aint_appcode,String astr_appmsg,String info,String input){
        ActiveXComponent  connect = new ActiveXComponent("YinHai.CHS.InterfaceSCS");
        Dispatch dis = connect.getObject();
        Variant v1 = Dispatch.call(dis,"yh_CHS_init",aint_appcode,astr_appmsg);
        System.out.println("-----------初始化返回值--------------:"+v1);
        String str="";
        Variant output = new Variant(str,true);//输出参数定义
        Dispatch.call(dis,"yh_CHS_call",info,input,output);
        System.out.println("-----------输出参数--------------:"+output);
        Dispatch.call(dis,"yh_CHS_destroy");
        return output.toString();
    }
}
