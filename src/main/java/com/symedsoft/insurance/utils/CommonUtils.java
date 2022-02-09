package com.symedsoft.insurance.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yx
 * @version 1.0.0
 * @Description
 * @createTime 2021年05月31日 09:46:00
 */
public class CommonUtils {
    // 从类unix机器上获取mac地址
//    public static String getMac(String ip) throws IOException {
//        String mac = SysCode.BDFH.EMPTY;
//        if (ip != null) {
//            try {
//                Process process = Runtime.getRuntime().exec("arp "+ip);
//                InputStreamReader ir = new InputStreamReader(process.getInputStream());
//                LineNumberReader input = new LineNumberReader(ir);
//                String line;
//                StringBuffer s = new StringBuffer();
//                while ((line = input.readLine()) != null) {
//                    s.append(line);
//                }
//                mac = s.toString();
//                if (StringUtils.isNotBlank(mac)) {
//                    mac = mac.substring(mac.indexOf(":") - 2, mac.lastIndexOf(":") + 3);
//                }
//                return mac;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        return mac;
//    }

    // 从windows机器上获取mac地址
    public static String getMacInWindows(final String ip) {
        String result = "";
        String[] cmd = {"cmd", "/c", "ping " + ip};
        String[] another = {"cmd", "/c", "ipconfig -all"};
        // 获取执行命令后的result
        String cmdResult = callCmd(cmd, another);
        // 从上一步的结果中获取mac地址
        result = filterMacAddress(ip, cmdResult, "-");
        return result;
    }

    // 命令执行
    public static String callCmd(String[] cmd, String[] another) {
        String result = "";
        String line = "";
        try {
            Runtime rt = Runtime.getRuntime();
            // 执行第一个命令
            Process proc = rt.exec(cmd);
            proc.waitFor();
            // 执行第二个命令
            proc = rt.exec(another);
            InputStreamReader is = new InputStreamReader(proc.getInputStream());
            BufferedReader br = new BufferedReader(is);
            while ((line = br.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    // 获取mac地址
    public static String filterMacAddress(final String ip, final String sourceString, final String macSeparator) {
        String result = "";
        String regExp = "((([0-9,A-F,a-f]{1,2}" + macSeparator + "){1,5})[0-9,A-F,a-f]{1,2})";
        Pattern pattern = Pattern.compile(regExp);
        Matcher matcher = pattern.matcher(sourceString);
        while (matcher.find()) {
            result = matcher.group(1);
            // 因计算机多网卡问题，截取紧靠IP后的第一个mac地址
            int num = sourceString.indexOf(ip) - sourceString.indexOf(": "+result + " ");
            if (num>0&&num<300) {
                break;
            }
        }
        return result;
    }
}
