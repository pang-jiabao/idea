package com.symedsoft.insurance.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 *@author：LL
 *@Date:2021/5/15
 *@Description list util
 */
public class ListUtils {

    /**
     * 切分list
     * @param sourceList
     * @param groupSize 每组定长
     * @return
     */
    public static <T> List<List<List<T>>> splitList(List<List<T>> sourceList, int groupSize) {
        int length = sourceList.size();
        // 计算可以分成多少组
        int num = (length + groupSize - 1) / groupSize;
        List<List<List<T>>> newList = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            // 开始位置
            int fromIndex = i * groupSize;
            // 结束位置
            int toIndex = (i + 1) * groupSize < length ? (i + 1) * groupSize : length;
            newList.add(sourceList.subList(fromIndex, toIndex));
        }
        return newList;
    }


    /**
     * 切分list
     * @param sourceList
     * @param groupSize 每组定长
     * @return
     */
    public static <T> List<List<Map<String,Object>>> splitList2(List<Map<String,Object>> sourceList, int groupSize) {
        int length = sourceList.size();
        // 计算可以分成多少组
        int num = (length + groupSize - 1) / groupSize;
        List<List<Map<String,Object>>> newList = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            // 开始位置
            int fromIndex = i * groupSize;
            // 结束位置
            int toIndex = (i + 1) * groupSize < length ? (i + 1) * groupSize : length;
            newList.add(sourceList.subList(fromIndex, toIndex));
        }
        return newList;
    }

    /**
     * 切分list
     * @param sourceList
     * @param groupSize 每组定长
     * @return
     */
    public static List<List<String>> splitList3(List<String> sourceList, int groupSize) {
        int length = sourceList.size();
        // 计算可以分成多少组
        int num = (length + groupSize - 1) / groupSize;
        List<List<String>> newList = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            // 开始位置
            int fromIndex = i * groupSize;
            // 结束位置
            int toIndex = (i + 1) * groupSize < length ? (i + 1) * groupSize : length;
            newList.add(sourceList.subList(fromIndex, toIndex));
        }
        return newList;
    }
}
