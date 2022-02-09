package com.symedsoft.insurance.service;

import javax.servlet.http.HttpServletRequest;

/**
 * @author yx
 * @version 1.0.0
 * @Description
 * @createTime 2021年05月27日 17:08:00
 */
public interface IGetMacAddressService {
    String getMacAddress(HttpServletRequest request);
}
