package com.symedsoft.insurance.utils;

import org.mybatis.generator.api.ShellRunner;

/*
 *@author：LL
 *@Date:2021/5/12
 *@Description
 */
public class GeneratorUtils {

    // 该配置文件放在src\\main\\resources\\该路径下即可
    public static void main(String[] args) {
        args = new String[] { "-configfile", "src\\main\\resources\\mybatis-generator.xml", "-overwrite" };
        ShellRunner.main(args);
    }

}
