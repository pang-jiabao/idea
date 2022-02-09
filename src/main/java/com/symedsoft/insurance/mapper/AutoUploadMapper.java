package com.symedsoft.insurance.mapper;

import com.symedsoft.insurance.vo.InsurBusinessConfigVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author yx
 * @version 1.0.0
 * @Description
 * @createTime 2021年05月24日 17:04:00
 */
@Mapper
public interface AutoUploadMapper {
    /**
     * 查询在院医保病人
     *
     * @return
     */
    List<Map<String, Object>> getInpAdmit();

    Map<String, Object> getInpAdmitByPatientId(@Param("patientId") String patientId, @Param("visitId") int visitId);

    List<Map<String, Object>> getInpDetailFeedetail(@Param("patientId") String patientId,
                                                    @Param("visitId") int visitId,
                                                    @Param("doctorCode") String doctorCode,
                                                    @Param("selfFlag") String selfFlag);

    /**
     * 查询住院费用上传入参信息，包含新生儿
     *
     * @param patientId
     * @param visitId
     * @param bronIds
     * @param doctorCode
     * @return
     */
    List<Map<String, Object>> getInpDetailFeedetailAndBron(@Param("patientId") String patientId,
                                                           @Param("visitId") int visitId,
                                                           @Param("bronIds") List<String> bronIds,
                                                           @Param("doctorCode") String doctorCode,
                                                           @Param("selfFlag") String selfFlag);


    String getSerialNo();

    int insertInpDetailFeedetail(Map<String, Object> map);

    int insertInpDetailFeedetailExp(Map<String, Object> map);

    int updateTransFlag(List<Object> list);

    int updateDetailFlag(@Param("patientId") String patientId, @Param("visitId") int visitId, @Param("itemNos") List<String> itemNos);

    /**
     * @param patientId
     * @param visitId
     * @return
     */
    List<String> getUploadSerialNo(@Param("patientId") String patientId,
                                   @Param("visitId") int visitId);

    List<Map<String, Object>> getInpDetailUpFeedetailIn(@Param("serialNo") String serialNo,
                                                        @Param("isRollBack") boolean isRollBack);

    int queryInpDetailUpFeedetailInCount(@Param("serialNo") String serialNo,
                                         @Param("isRollBack") boolean isRollBack);

    InsurBusinessConfigVO getInpFeeInterfaceConfig();

    Map<String, Object> getInpDetailUpFeedetailInExp(@Param("nodeNum") Object nodeNum);

    Map<String, Object> getPatientInfo(String serialNo);

    List<String> getBronInfo(@Param("patientId") String patientId, @Param("visitId") int visitId);

    List<Map<String, Object>> getInpBillDetail(@Param("patientId") String patientId, @Param("visitId") int visitId);

    List<Map<String, Object>> getInsurVsClinic(@Param("itemCode") Object itemCode, @Param("itemSpec") Object itemSpec);

    List<Map<String, Object>> queryRepeatDrugPrice();

    List<Map<String, Object>> queryCutDownInpDetail(@Param("pric") BigDecimal pric,
                                                    @Param("medinsListCodg") String medinsListCodg,
                                                    @Param("serialNo") String serialNo);

    List<Map<String, Object>> queryCutDownOneInpDetail(@Param("totalCosts") BigDecimal totalCosts,
                                                       @Param("ctn") BigDecimal ctn,
                                                       @Param("medinsListCodg") String medinsListCodg,
                                                       @Param("serialNo") String serialNo);
    List<Map<String, Object>> queryCutDownEqualsInpDetail(@Param("totalCosts") BigDecimal totalCosts,
                                                          @Param("ctn") BigDecimal ctn,
                                                          @Param("pric") BigDecimal pric,
                                                          @Param("medinsListCodg") String medinsListCodg,
                                                          @Param("serialNo") String serialNo);
    int modifyInpDetailRecord(@Param("rollbackRemaining") BigDecimal rollbackRemaining,
                              @Param("id") String id);

    BigDecimal getPackPrice(@Param("itemCode") Object itemCode, @Param("itemUnit") Object itemUnit
            ,@Param("firmId")String firmId,@Param("billingDate")Object billingDate
            ,@Param("minSpec")String minSpec);

    int getCountPositiveFee(@Param("patientId") String patientId, @Param("visitId") int visitId);

    Map<String, String> getMinUnits(@Param("itemCode") Object itemCode, @Param("itemSpec") Object itemSpec,@Param("billingDate")Object billingDate);

    Map<String, Object> getInpBillDetailOne(@Param("patientId") String patientId, @Param("visitId") int visitId, @Param("itemNo") int itemNo);

    Map<String,String> getItemUnitsFromInsurVsClinic(@Param("itemCode")String itemCode,@Param("itemSpec")String itemSpec,@Param("units")String units,@Param("itemClass")String itemClass);
    //中药颗粒转自费
    int getZyCount(String itemCode);


    List<Map<String, Object>> getPreSettleInput(@Param("patientId") String patientId,
                                                @Param("visitId") int visitId);

    int deletePreSettleDataIn(@Param("patientId") String patientId,
                            @Param("visitId") int visitId);

    int deletePreSettleSetldetailOut(@Param("patientId") String patientId,
                              @Param("visitId") int visitId);

    int deletePreSettleSetlinfoOut(@Param("patientId") String patientId,
                              @Param("visitId") int visitId);

    String getMedType(@Param("patientId") String patientId,
                      @Param("visitId") int visitId);
}
