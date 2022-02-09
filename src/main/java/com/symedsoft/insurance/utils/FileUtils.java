package com.symedsoft.insurance.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 1、将文件转为base64的工具方法<br/>
 * 2、读取文件数据，并生成为字符串集合
 */
public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    /**
     * 文件转 base64 字符串
     * @param path 文件全路径(加文件名)
     * @return base64 字符串
     */
    public static String file2Base64(String path) {
        String base64 = null;
        File file = new File(path);
        try (InputStream in = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            in.read(bytes);
            base64 = new String(Base64.encodeBase64(bytes),"UTF-8");
            logger.info("将文件:{} 转base64字符串: {}" , path , base64);
        } catch (Exception e) {
            logger.info("解压文件{}出错，具体异常为：{}" , path , e);
        }
        return base64;
    }

    /**
     * 将base64 字符串转换为文件
     * @param outFilePath  输出文件路径
     * @param base64   base64文件编码字符串
     * @param outFileName  输出文件名
     * @return String
     * @description BASE64解码成File文件
     */
    public static void base2File(String outFilePath , String base64 , String outFileName) {
        logger.info("BASE64:{},解码成File文件{}." , base64 , outFileName + File.separator + outFileName);
        //创建文件目录
        String filePath = outFilePath;
        File dir=new File(filePath);
        if (!dir.exists() && !dir.isDirectory()) {
            dir.mkdirs();
        }

        File file = new File(filePath + File.separator + outFileName);
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            byte[] bytes = Base64.decodeBase64(base64);
            bos.write(bytes);
        } catch (Exception e) {
            logger.info("转换成路径:{}下的文件:{}出错，异常信息为:{}" , outFileName , outFileName, e);
        }
    }

    /**
     * 读取文件信息到 嵌套 list 中
     * @param path
     * @return
     */
    public static List<List<String>> readFile2List(String path) {
        String line = null;
        List<List<String>> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            while ((line = br.readLine()) != null) {
                List<String> record = new ArrayList<>();
                String[] data = line.split("\t");
                for (String s : data) {
                    if (StringUtils.isNotEmpty(s)) {
                        record.add(s);
                    }
                }

                list.add(record);
            }
        } catch (Exception e) {
            logger.info("读取文件:{}出错，具体异常信息为;{}" ,path ,e);
        }

        return list;
    }

    /**
     * 将list 转成txt文件<br/>
     * 转换失败返回null，成功则返回文件路径
     * @param list
     * @param keySort 读写字段顺序
     * @param path 写文件的全路径
     * @return
     */
    public static boolean writeList2File(List<Map<String , Object>> list , List<String> keySort , String path) {
        File outFile = new File(path);
        boolean flag = true;

        try (OutputStream fileOutputStream = new FileOutputStream(outFile,false);
             OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "utf-8");
             Writer out = new BufferedWriter(outputStreamWriter, 10240)){
            for (Map<String , Object> map : list) {
                for (String key : keySort) {
                    if (map.containsKey(key)) {
                        out.write(MapUtils.getObject2String(map , key));
                    } else {
                        out.write("");
                    }
                    out.write("\t");
                }
                out.write("\r\n");
            }
        } catch (UnsupportedEncodingException e) {
            flag = false;
            logger.error("不支持的字符集：UTF-8");
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            flag = false;
            logger.error("文件未找到：{}" , e);
        } catch (IOException e) {
            flag = false;
            logger.error("IO异常：{}" , e);
        }
        return flag ;
    }

    /**
     * 单元测试
     */
    public static void main(String[] args) {

        //定义文件路径
        String filePath = "C:\\porject\\insurance\\hs_err_pid15748.log";
        //将文件转base64字符串
        String base64 = file2Base64(filePath);

        System.out.println(base64);
        //定义输出文件的路径outFilePath和输出文件名outoutFileName
        String outFilePath = "C:\\porject\\insurance";
        String outFileName = "test.log";
        //将BASE64解码成File文件
        base2File(outFilePath, base64, outFileName);

    }

}
