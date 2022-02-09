package com.symedsoft.insurance.utils;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class IPUtils {
    private static Logger logger = LoggerFactory.getLogger(IPUtils.class);

    /**
     * 获取IP地址
     *
     * 使用Nginx等反向代理软件， 则不能通过request.getRemoteAddr()获取IP地址
     * 如果使用了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP地址，X-Forwarded-For中第一个非unknown的有效IP字符串，则为真实IP地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        String ip = null;
        try {
            ip = request.getHeader("x-forwarded-for");
            if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (StringUtils.isEmpty(ip) || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
        } catch (Exception e) {
            logger.error("IPUtil ERROR ", e);
        }

//        //使用代理，则获取第一个IP地址
//        if(StringUtils.isEmpty(ip) && ip.length() > 15) {
//			if(ip.indexOf(",") > 0) {
//				ip = ip.substring(0, ip.indexOf(","));
//			}
//		}

        return ip;
    }

    public static List<List<Integer>> fourSum(int[] nums, int target) {
        List<List<Integer>> ret = new ArrayList<>();
        //特殊情况判断
        if (nums.length < 4) {
            return new ArrayList<>();
        } else if (nums.length == 4) {
            if (nums[0] + nums[1] + nums[2] + nums[3] == target) {
                List<Integer> list = new ArrayList<>();
                list.add(nums[0]);
                list.add(nums[1]);
                list.add(nums[2]);
                list.add(nums[3]);
                ret.add(list);
                return ret;
            } else {
                return new ArrayList<>();
            }
        }

        List<Integer> addC = new ArrayList<>();

        for (int i = 0; i < nums.length - 3; i ++) {
            for (int j = i + 1; j < nums.length - 2; j ++) {
                for (int k = j + 1; k < nums.length - 1; k ++) {
                    for (int l = k + 1; l < nums.length; l ++) {
                        if (nums[i] + nums[j] + nums[k] + nums[l] == target) {
                            List<Integer> list = new ArrayList<>();
                            list.add(nums[i]);
                            System.out.print(nums[i]);
                            list.add(nums[j]);
                            System.out.print("," + nums[j]);
                            list.add(nums[k]);
                            System.out.print("," + nums[k]);
                            list.add(nums[l]);
                            System.out.print("," + nums[l]);
                            System.out.println();
                            if (ret.size() == 0) {
                                ret.add(list);
                            } else if (!ret.contains(list)) {
                                ret.add(list);
                            }
                        }
                    }
                }
            }
        }
        return ret;
    }


    //获取服务器地址
    public static String getLocalIP() {
        String sIP = "";
        InetAddress ip = null;
        try {
            boolean bFindIP = false;
            Enumeration<NetworkInterface> netInterfaces = (Enumeration<NetworkInterface>) NetworkInterface
                    .getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                if (bFindIP) {
                    break;
                }
                NetworkInterface ni = (NetworkInterface) netInterfaces.nextElement();
                Enumeration<InetAddress> ips = ni.getInetAddresses();
                while (ips.hasMoreElements()) {
                    ip = (InetAddress) ips.nextElement();
                    if (!ip.isLoopbackAddress() && ip.getHostAddress().matches("(\\d{1,3}\\.){3}\\d{1,3}")) {
                        bFindIP = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        if (null != ip) {
            sIP = ip.getHostAddress();
        }
        return sIP;
    }

    public static void main(String[] args) {
        int[] x = {-2,-1,-1,1,1,2,2};
        List<List<Integer>> as = fourSum(x , 0);
    }
}
