package com.symedsoft.insurance.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.Scanner;
import java.util.regex.Pattern;

/*
 *@author：LL
 *@Date:2021/4/13
 *@Description 汉字转换拼音工具类
 */
public class PinyinUtils {


    /**
     * 获取字符串拼音的第一个字母
     * @param chinese
     * @param type 大小写
     * @return 例子： 周树人==> zsr
     */
    public static String toFirstChar(String chinese,HanyuPinyinCaseType type){
        String pinyinStr = "";
        char[] newChar = chinese.toCharArray();  //转为单个字符
        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setCaseType(type);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        try {
            for (int i = 0; i < newChar.length; i++) {
                if (newChar[i] == '：') {
                    pinyinStr += newChar[i];
                }else if (String.valueOf(newChar[i]).matches("[\u4e00-\u9fa5]+")){
                    pinyinStr += PinyinHelper.toHanyuPinyinStringArray(newChar[i], defaultFormat)[0].charAt(0);
                } else if (Pattern.matches("\\d+", String.valueOf(newChar[i]))) {
                    pinyinStr += newChar[i];
                } else {
                    pinyinStr += newChar[i];
                }
            }
        } catch (Exception e) {
            System.out.println("字符不能转成汉语拼音");
        }

        return pinyinStr;
    }

    /**
     * 汉字转为拼音
     * @param chinese 待转字符串
     * @param type 转换类型 大写 小写
     * @return 全拼音 例：周树人==>ZHOUSHUREN
     */
    public static String toPinyinbyType(String chinese,HanyuPinyinCaseType type){
        String pinyinStr = "";
        char[] newChar = chinese.toCharArray();
        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setCaseType(type);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        for (int i = 0; i < newChar.length; i++) {
            if (newChar[i] > 128) {
                try {
                    pinyinStr += PinyinHelper.toHanyuPinyinStringArray(newChar[i], defaultFormat)[0];
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    e.printStackTrace();
                }
            }else{
                pinyinStr += newChar[i];
            }
        }
        return pinyinStr;
    }

    /**
     * 汉字转换成拼音，空格间隔
     * @param chinese 待转换字符串
     * @param type 转换类型 大写 小写
     * @return 例：周树人==>ZHOU SHU REN
     */
    public static String toPinyinWithSpacebyType(String chinese,HanyuPinyinCaseType type){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chinese.length() ; i++) {
            String s = chinese.substring(i, i+1);
            sb.append(toPinyinbyType(s,HanyuPinyinCaseType.UPPERCASE)+" ");
        }
        return sb.toString();
    }

    public static void main(String[]args)
    {
        Scanner sc = new Scanner(System.in);
        int i = 0;
        while(sc.hasNext())
        {
            String s = sc.nextLine();
//            int x = Integer.valueOf(s);
            String x = toFirstChar(s , HanyuPinyinCaseType.LOWERCASE);
            System.out.println("第" + ++i + "个：" + x);
//            System.out.println(x);
        }
    }

}
