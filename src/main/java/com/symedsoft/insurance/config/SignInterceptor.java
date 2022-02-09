package com.symedsoft.insurance.config;

import com.symedsoft.insurance.mapper.InsuranceInterfaceMapper;
import com.symedsoft.insurance.service.SignService;
import org.apache.commons.lang.StringUtils;
import org.mybatis.logging.Logger;
import org.mybatis.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class SignInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(SignInterceptor.class);

    @Autowired
    private SignService signService;

    @Autowired
    private InsuranceInterfaceMapper interfaceMapper;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if (SignNoCache.cache.isEmpty() || !SignNoCache.cache.containsKey("sign_no")) {
            String operationNo = (String) request.getAttribute("operationNo");
            if (StringUtils.isEmpty(operationNo)) {
                operationNo = "001";
            }
            signService.signIn(operationNo);
        }
        return true;
    }
}
