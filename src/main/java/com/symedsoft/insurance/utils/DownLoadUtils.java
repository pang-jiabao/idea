package com.symedsoft.insurance.utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 东软接口公用下载工具类
 *
 */
public class DownLoadUtils {

    /**
     * 下载工具类，生成指定目录的文件
     * @param url 下载地址
     * @param jsonStr 请求入参 json 字符串
     * @param filename 生成文件的文件名
     * @param path 生成文件的本地路径
     */
    public static void downloadFileFromInsurance(String url , String jsonStr , String filename , String path) {
        HttpPost httppost = new HttpPost(url);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10000)
                .setSocketTimeout(10000).build();
        httppost.setConfig(requestConfig);
        ByteArrayEntity entity = new
                ByteArrayEntity(jsonStr.getBytes(StandardCharsets.UTF_8));
        entity.setContentType("text/plain");
        httppost.setEntity(entity);
//        CloseableHttpResponse response = null;
        try (CloseableHttpClient httpclient = HttpClients.createDefault();
             CloseableHttpResponse response = httpclient.execute(httppost)) {
//            response = httpclient.execute(httppost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                httppost.abort();
                throw new RuntimeException("HttpClient,error status code :" + statusCode);
            }
            HttpEntity responseEntity = response.getEntity();
            String result;
            if (responseEntity != null) {
                if (responseEntity.getContentType().getValue().contains("application/octet-stream")) {
                    InputStream content = responseEntity.getContent();
                    //返回文件流
                    File file = new File(path + File.separator + filename);
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    int temp;
                    while ((temp = content.read()) != -1) {
                        fileOutputStream.write(temp);
                    }
                    fileOutputStream.close();
                } else {
                    //返回字符串
                    result = EntityUtils.toString(responseEntity, "UTF-8");
                    System.out.println(result);
                }
            }
            EntityUtils.consume(entity);
        } catch (ClientProtocolException e) {
            throw new RuntimeException("提交给服务器的请求，不符合 HTTP 协议", e);
        } catch (IOException e) {
            throw new RuntimeException("向服务器承保接口发起 http 请求,执行 post 请求异常", e);
        }
    }
}
