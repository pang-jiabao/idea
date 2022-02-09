package com.symedsoft.insurance.controller;

import cn.hutool.core.date.DateTime;

import java.text.SimpleDateFormat;
import java.util.*;

public class Test {
    public static void main(String[] args) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmsss");
        String dateTime = sdf.format(new Date());
        System.out.println(dateTime);

    }
}
