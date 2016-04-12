package com.bwts.oestest.service;

import com.bwts.oestest.dto.OESSeal;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nisec.oes.Oes;
import com.nisec.oes.OesAllocator;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import stormpot.BlazePool;
import stormpot.Config;
import stormpot.Pool;
import stormpot.Timeout;

import java.io.UnsupportedEncodingException;
import java.security.DigestException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class EstampService {
    private static final String docProperty = "BAIWANG_EINVOICE_DOC";
    private static Logger LOGGER = LoggerFactory.getLogger(EstampService.class);
    private final Pool<Oes> remoteOesPool;
    private final Oes localOesObj;
    private static Timeout timeout;
    private final ThreadLocal<SimpleDateFormat> dateFormatThreadLocal = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyMMddHHmmss'Z'");
        }
    };

    private final ObjectMapper objectMapper;

    private ExecutorService es = Executors.newCachedThreadPool();

    public EstampService() {
        Oes.OES_Init();
        Config<Oes> config = new Config<>().setAllocator(new OesAllocator()).setSize(5);
        remoteOesPool = new BlazePool<>(config);
        timeout = new Timeout(3, TimeUnit.MINUTES);
        localOesObj = new Oes();
        this.objectMapper = new ObjectMapper();
    }

    public byte[] getSignValue(String sealId, byte[] pdf) {
        byte[] result = null;
        try {
            long start = System.currentTimeMillis();
            OesSealObj seal = getSeal(sealId);//remote
            LOGGER.info("getSeal {}", System.currentTimeMillis() - start);

            start = System.currentTimeMillis();
            OESSeal sealInfo = getSealInfo(seal.getSeal());//local
            LOGGER.info("getSealInfo {}", System.currentTimeMillis() - start);

            String signMethod = sealInfo.getSignMethod();

            start = System.currentTimeMillis();
            String digestMethod = getDigestMethod(); //local
            LOGGER.info("getDigestMethod {}", System.currentTimeMillis() - start);

            start = System.currentTimeMillis();
            String curDateString = dateFormatThreadLocal.get().format(new Date());
            //byte[] date = getSignDateTime();//remote
            LOGGER.info("getSignDateTime {}", System.currentTimeMillis() - start);

            start = System.currentTimeMillis();
            byte[] data = digest(pdf, digestMethod);//local,time consuming
            LOGGER.info("digest {}", System.currentTimeMillis() - start);

            start = System.currentTimeMillis();
            byte[] sig = sign(sealId, docProperty, data, signMethod, curDateString.getBytes());//remote
            LOGGER.info("sign {}", System.currentTimeMillis() - start);
            result = encodeDateToSig(curDateString, sig);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public byte[] encodeDateToSig(String dateString, byte[] sig) throws Exception {
        String sig64 = Base64.encodeBase64String(sig);
        HashMap<String, String> map = new HashMap<>();
        map.put("date", dateString);
        map.put("sig", sig64);
        return objectMapper.writeValueAsBytes(map);
    }

    public Map<String, String> parseDateAndSig(byte[] dataBlock) throws Exception {
        return objectMapper.readValue(dataBlock, Map.class);
    }


    public BAWrapper getImage(String sealId) {
        byte[] image = null;
            try {
                OesSealObj seal = getSeal(sealId);
                image = getSealImage(seal.getSeal(), seal.getLength(), 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        BAWrapper wrapper = new BAWrapper();
        wrapper.setData(image);
        return wrapper;
    }

    public boolean verify(String sealId, byte[] dataBlock, byte[] dataToBeVerfied) {
        boolean flag = false;
        try {
            OesSealObj seal = getSeal(sealId);
            OESSeal sealInfo = getSealInfo(seal.getSeal());
            String signMethod = sealInfo.getSignMethod();
            String digestMethod = getDigestMethod();
            Map<String, String> map = parseDateAndSig(dataBlock);
            String dateString = map.get("date");
            String sigString = map.get("sig");
            byte[] sig = Base64.decodeBase64(sigString);
            byte[] data = digest(dataToBeVerfied, digestMethod);

            flag = verify(seal.getSeal(), docProperty, data, signMethod, dateString.getBytes(), sig, 0);
        } catch (Exception e) {
            LOGGER.info("Verify signature failed.");
        }
        return flag;
    }

//    public void checkSealIdValid(String sealId) {
//        try {
//            OesSealObj seal = getSeal(sealId);
//            if (seal == null) {
//                throw new APIException(HttpStatus.NOT_FOUND, ErrorCodes.SEAL_ID_NOT_EXISTS);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void checkEstampAlive() {
//        try {
//            Future<Boolean> future = es.submit(this::testSealList);
//            future.get(3000, TimeUnit.MILLISECONDS);
//        } catch (Exception e) {
//            throw new APIException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCodes.ESTAMP_NOT_RESPOND);
//        }
//    }

    private Boolean testSealList() throws Exception{
        int ret;
        int[] iSealListDataLen = new int[1];
        iSealListDataLen[0] = 0;
        byte[] uchSealListData = new byte[1];
        Oes oes_api = remoteOesPool.claim(timeout);
        try {
            ret = oes_api.OES_GetSealList(uchSealListData, iSealListDataLen);
            if (ret != 0) {
                LOGGER.info("OES_GetSealList= {} ", ret);
                return false;
            }
            uchSealListData = new byte[iSealListDataLen[0]];
            ret = oes_api.OES_GetSealList(uchSealListData, iSealListDataLen);
            if (ret != 0) {
                LOGGER.info("OES_GetSealList = {}", ret);
                return false;
            }
            return true;
        } finally {
            if (oes_api != null) {
                oes_api.release();
            }
        }

    }

    private byte[] sign(String sealId, String docProperty, byte[] data, String method, byte[] datetime) throws Exception {
        int ret;
        byte[] bytesealId = sealId.getBytes("utf-8");
        byte[] bytedocProperty = docProperty.getBytes("utf-8");
        byte[] bytemethod = method.getBytes("utf-8");
        int[] iSignValueLen = new int[]{0};
        byte[] uchSignValue = new byte[1];
        Oes oes_api = remoteOesPool.claim(timeout);
        try {
            ret = (int) oes_api.OES_Sign(bytesealId, bytesealId.length, bytedocProperty, bytedocProperty.length, data, data.length, bytemethod, bytemethod.length, datetime, datetime.length, uchSignValue, iSignValueLen);
            if (ret != 0) {
                LOGGER.error("OES_Sign #1 failed, ret={}", ret);
                throw new RuntimeException("OES_Sign #1 failed, ret=" + ret);
            }
            uchSignValue = new byte[iSignValueLen[0]];
            ret = (int) oes_api.OES_Sign(bytesealId, bytesealId.length, bytedocProperty, bytedocProperty.length, data, data.length, bytemethod, bytemethod.length, datetime, datetime.length, uchSignValue, iSignValueLen);
            if (ret != 0) {
                LOGGER.error("OES_Sign #2 failed, ret={}", ret); //stuck
                throw new RuntimeException("OES_Sign #2 failed, ret=" + ret);
            }
            byte[] result = uchSignValue;
            if (uchSignValue.length > iSignValueLen[0]) {
                result = new byte[iSignValueLen[0]];
                System.arraycopy(uchSignValue, 0, result, 0, iSignValueLen[0]);
            }
            return result;
        } finally {
            if (oes_api != null) {
                oes_api.release();
            }
        }
    }

    private OesSealObj getSeal(String sealId) throws Exception {
        int ret;
        int[] iSealDataLen = new int[]{0};
        byte[] uchSealData = new byte[1];
        byte[] sealIdByte2 = sealId.getBytes("utf-8");
        Oes oes_api = remoteOesPool.claim(timeout);
        try {
            ret = oes_api.OES_GetSeal(sealIdByte2, sealIdByte2.length, uchSealData, iSealDataLen);
            if (ret != 0) {
                LOGGER.error("OES_GetSeal #1 failed, ret={}", ret);
                return null;
            } else {
                uchSealData = new byte[iSealDataLen[0]];
                ret = oes_api.OES_GetSeal(sealIdByte2, sealIdByte2.length, uchSealData, iSealDataLen);
                if (ret != 0) {
                    LOGGER.error("OES_GetSeal #2 failed, ret={}", ret);
                    return null;
                } else {
                    byte[] result = uchSealData;
                    if (uchSealData.length > iSealDataLen[0]) {
                        result = new byte[iSealDataLen[0]];
                        System.arraycopy(uchSealData, 0, result, 0, iSealDataLen[0]);
                    }
                    OesSealObj oesSealObj = new OesSealObj();
                    oesSealObj.setSeal(result);
                    oesSealObj.setLength(iSealDataLen[0]);
                    return oesSealObj;
                }
            }
        } finally {
            if (oes_api != null) {
                oes_api.release();
            }
        }
    }

    private OESSeal getSealInfo(byte[] seal) throws Exception {
        int ret;
        int[] iSealIdLen = new int[]{0};
        byte[] uchSealId = new byte[1];
        int[] iVersionLen = new int[]{0};
        byte[] uchVersion = new byte[1];
        int[] iVenderIdLen = new int[]{0};
        byte[] uchVenderId = new byte[1];
        int[] iSealTypeLen = new int[]{0};
        byte[] uchSealType = new byte[1];
        int[] iSealNameLen = new int[]{0};
        byte[] uchSealName = new byte[1];
        int[] iCertInfoLen = new int[]{0};
        byte[] uchCertInfo = new byte[1];
        int[] iValidStartLen = new int[]{0};
        byte[] uchValidStart = new byte[1];
        int[] iValidEndLen = new int[]{0};
        byte[] uchValidEnd = new byte[1];
        int[] iSignedDateLen = new int[]{0};
        byte[] uchSignedDate = new byte[1];
        int[] iSignerNameLen = new int[]{0};
        byte[] uchSignerName = new byte[1];
        int[] iSignMethodLen = new int[]{0};
        byte[] uchSignMethod = new byte[1];
        ret = localOesObj
                .OES_GetSealInfo(seal, seal.length, uchSealId, iSealIdLen, uchVersion, iVersionLen, uchVenderId,
                        iVenderIdLen, uchSealType, iSealTypeLen, uchSealName, iSealNameLen, uchCertInfo, iCertInfoLen,
                        uchValidStart, iValidStartLen, uchValidEnd, iValidEndLen, uchSignedDate, iSignedDateLen,
                        uchSignerName, iSignerNameLen, uchSignMethod, iSignMethodLen);
        if (ret != 0) {
            LOGGER.error("OES_GetSealInfo #1 failed, ret={}", ret);
            return null;
        } else {
            uchSealId = new byte[iSealIdLen[0]];
            uchVersion = new byte[iVersionLen[0]];
            uchVenderId = new byte[iVenderIdLen[0]];
            uchSealType = new byte[iSealTypeLen[0]];
            uchSealName = new byte[iSealNameLen[0]];
            uchCertInfo = new byte[iCertInfoLen[0]];
            uchValidStart = new byte[iValidStartLen[0]];
            uchValidEnd = new byte[iValidEndLen[0]];
            uchSignedDate = new byte[iSignedDateLen[0]];
            uchSignerName = new byte[iSignerNameLen[0]];
            uchSignMethod = new byte[iSignMethodLen[0]];
            ret = localOesObj
                    .OES_GetSealInfo(seal, seal.length, uchSealId, iSealIdLen, uchVersion, iVersionLen, uchVenderId,
                            iVenderIdLen, uchSealType, iSealTypeLen, uchSealName, iSealNameLen, uchCertInfo,
                            iCertInfoLen, uchValidStart, iValidStartLen, uchValidEnd, iValidEndLen, uchSignedDate,
                            iSignedDateLen, uchSignerName, iSignerNameLen, uchSignMethod, iSignMethodLen);
        }
        if (ret != 0) {
            LOGGER.error("OES_GetSealInfo #2 failed, ret={}", ret);
            return null;
        } else {
            byte[] resultSealId = uchSealId;
            if (uchSealId.length > iSealIdLen[0]) {
                resultSealId = new byte[iSealIdLen[0]];
                System.arraycopy(uchSealId, 0, resultSealId, 0, iSealIdLen[0]);
            }

            byte[] resultSealName = uchSealName;
            if (uchSealName.length > iSealNameLen[0]) {
                resultSealName = new byte[iSealNameLen[0]];
                System.arraycopy(uchSealName, 0, resultSealName, 0, iSealNameLen[0]);
            }

            byte[] resultVersion = uchVersion;
            if (uchVersion.length > iVersionLen[0]) {
                resultVersion = new byte[iVersionLen[0]];
                System.arraycopy(uchVersion, 0, resultVersion, 0, iVersionLen[0]);
            }

            byte[] resultVenderId = uchVenderId;
            if (uchVenderId.length > iVenderIdLen[0]) {
                resultVenderId = new byte[iVenderIdLen[0]];
                System.arraycopy(uchVenderId, 0, resultVenderId, 0, iVenderIdLen[0]);
            }

            byte[] resultSealType = uchSealType;
            if (uchSealType.length > iSealTypeLen[0]) {
                resultSealType = new byte[iSealTypeLen[0]];
                System.arraycopy(uchSealType, 0, resultSealType, 0, iSealTypeLen[0]);
            }

            byte[] resultCertInfo = uchCertInfo;
            if (uchCertInfo.length > iCertInfoLen[0]) {
                resultCertInfo = new byte[iCertInfoLen[0]];
                System.arraycopy(uchCertInfo, 0, resultCertInfo, 0, iCertInfoLen[0]);
            }

            byte[] resultValidStart = uchValidStart;
            if (uchValidStart.length > iValidStartLen[0]) {
                resultValidStart = new byte[iValidStartLen[0]];
                System.arraycopy(uchValidStart, 0, resultValidStart, 0, iValidStartLen[0]);
            }

            byte[] resultValidEnd = uchValidEnd;
            if (uchValidEnd.length > iValidEndLen[0]) {
                resultValidEnd = new byte[iValidEndLen[0]];
                System.arraycopy(uchValidEnd, 0, resultValidEnd, 0, iValidEndLen[0]);
            }

            byte[] resultSignDate = uchSignedDate;
            if (uchSignedDate.length > iSignedDateLen[0]) {
                resultSignDate = new byte[iSignedDateLen[0]];
                System.arraycopy(uchSignedDate, 0, resultSignDate, 0, iSignedDateLen[0]);
            }

            byte[] resultSignerName = uchSignerName;
            if (uchSignerName.length > iSignerNameLen[0]) {
                resultSignerName = new byte[iSignerNameLen[0]];
                System.arraycopy(uchSignerName, 0, resultSignerName, 0, iSignerNameLen[0]);
            }

            byte[] resultSignMethod = uchSignMethod;
            if (uchSignMethod.length > iSignMethodLen[0]) {
                resultSignMethod = new byte[iSignMethodLen[0]];
                System.arraycopy(uchSignMethod, 0, resultSignMethod, 0, iSignMethodLen[0]);
            }

            OESSeal oesSeal = new OESSeal();
            oesSeal.setId(new String(resultSealId, "utf-8"));
            oesSeal.setName(new String(resultSealName, "utf-8"));
            oesSeal.setSignerName(new String(resultSignerName, "utf-8"));
            oesSeal.setType(new String(resultSealType, "utf-8"));
            oesSeal.setVenderId(new String(resultVenderId, "utf-8"));
            oesSeal.setSignMethod(new String(resultSignMethod, "utf-8"));
            oesSeal.setVersion(new String(resultVersion, "utf-8"));
            oesSeal.setCertInfo(new String(resultCertInfo, "utf-8"));
            oesSeal.setSignDate(resultSignDate);
            oesSeal.setValidEnd(resultValidEnd);
            oesSeal.setValidStart(resultValidStart);
            return oesSeal;
        }
    }

    private String getDigestMethod() {
        int ret;
        int[] iDigestMethodLen = new int[]{0};
        byte[] uchDigestMethod = new byte[1];
        try {
            ret = localOesObj.OES_GetDigestMethod(uchDigestMethod, iDigestMethodLen);
            if (ret == 0) {
                uchDigestMethod = new byte[iDigestMethodLen[0]];
                ret = localOesObj.OES_GetDigestMethod(uchDigestMethod, iDigestMethodLen);
                if (ret == 0) {
                    byte[] result = uchDigestMethod;
                    if (uchDigestMethod.length > iDigestMethodLen[0]) {
                        result = new byte[iDigestMethodLen[0]];
                        System.arraycopy(uchDigestMethod, 0, result, 0, iDigestMethodLen[0]);
                    }

                    String strDigestMethod = null;

                    try {
                        strDigestMethod = new String(result, "utf-8");
                    } catch (UnsupportedEncodingException var6) {
                        LOGGER.error("byte to string error " + var6.getMessage());
                    }
                    return strDigestMethod;
                }
                throw new DigestException("OES_GetDigestMethod #2 failed, ret=" + ret);
            }
            throw new DigestException("OES_GetDigestMethod #1 failed, ret=" + ret);
        } catch (DigestException e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }

    private byte[] getSignDateTime() throws Exception{
        int ret;
        int[] iSignDateTimeLen = new int[]{0};
        byte[] uchSignDateTime = new byte[1];
        Oes oes_api = remoteOesPool.claim(timeout);
        try {
            ret = oes_api.OES_GetSignDateTime(uchSignDateTime, iSignDateTimeLen);
            if (ret != 0) {
                LOGGER.error("OES_GetSignDateTime #1 failed, ret={}", ret);
                return null;
            } else {
                uchSignDateTime = new byte[iSignDateTimeLen[0]];
                ret = oes_api.OES_GetSignDateTime(uchSignDateTime, iSignDateTimeLen);
                if (ret != 0) {
                    LOGGER.error("OES_GetSignDateTime #2 failed, ret={}", ret);
                    return null;
                } else {
                    byte[] result = uchSignDateTime;
                    if (uchSignDateTime.length > iSignDateTimeLen[0]) {
                        result = new byte[iSignDateTimeLen[0]];
                        System.arraycopy(uchSignDateTime, 0, result, 0, iSignDateTimeLen[0]);
                    }
                    return result;
                }
            }
        } finally {
            if (oes_api != null) {
                oes_api.release();
            }
        }
    }

    private byte[] digest(byte[] data, String method) throws Exception {
        int ret;
        byte[] uchDigestMethod = method.getBytes("utf-8");
        int iDigestMethodLen = uchDigestMethod.length;
        int[] iDigestValueLen = new int[]{0};
        byte[] uchDigestValue = new byte[1];
        ret = (int) localOesObj
                .OES_Digest(data, data.length, uchDigestMethod, iDigestMethodLen, uchDigestValue, iDigestValueLen);
        if (ret != 0) {
            LOGGER.error("OES_Digest #1 failed" + ret);
            throw new RuntimeException("OES_Digest #1 failed, ret=" + ret);
        }

        uchDigestValue = new byte[iDigestValueLen[0]];
        ret = (int) localOesObj
                .OES_Digest(data, data.length, uchDigestMethod, iDigestMethodLen, uchDigestValue, iDigestValueLen);
        if (ret != 0) {
            LOGGER.error("OES_Digest #2 failed, ret={}", ret);
        }

        byte[] result = uchDigestValue;
        if (uchDigestValue.length > iDigestValueLen[0]) {
            result = new byte[iDigestValueLen[0]];
            System.arraycopy(uchDigestValue, 0, result, 0, iDigestValueLen[0]);
        }
        return result;
    }

    public byte[] getSealImage(byte[] seal, int length, int renderFlag) throws Exception {
        int ret;
        byte[] uchSealImage = new byte[1];
        int[] iSealImageLen = new int[1];
        iSealImageLen[0] = 0;
        int[] iSealWidth = new int[1];
        iSealWidth[0] = 0;
        int[] iSealHeight = new int[1];
        iSealHeight[0] = 0;
        Oes oes_api = remoteOesPool.claim(timeout);
        if (null == oes_api) {
            System.out.println("no usable oes object! ");
        }
        try {
            ret = oes_api
                    .OES_GetSealImage(seal, length, renderFlag, uchSealImage, iSealImageLen, iSealWidth, iSealHeight);
            if (ret != 0) {
                LOGGER.error("OES_GetSealImageuchSealData #1 failed, ret={}", ret);
                return null;
            }

            LOGGER.info("iSealImageLen=" + iSealImageLen[0]);
            uchSealImage = new byte[iSealImageLen[0]];
            ret = oes_api
                    .OES_GetSealImage(seal, length, renderFlag, uchSealImage, iSealImageLen, iSealWidth, iSealHeight);
            if (ret != 0) {
                LOGGER.error("OES_GetSealImageuchSealData #2 failed, ret={}", ret);
                return null;
            }
            return uchSealImage;
        } finally {
            if (oes_api != null) {
                oes_api.release();
                Thread.sleep(1);
            }
        }
    }

    private boolean verify(byte[] seal, String docProperty, byte[] data, String method, byte[] datetime,
                           byte[] signValue, int online) throws Exception {
        int ret;
        byte[] bytemethod = method.getBytes("utf-8");
        byte[] bytedocProperty = docProperty.getBytes("utf-8");
        Oes oes_api = remoteOesPool.claim(timeout);
        try {
            ret = (int) oes_api
                    .OES_Verify(seal, seal.length, bytedocProperty, bytedocProperty.length, data, data.length,
                            bytemethod, bytemethod.length, datetime, datetime.length, signValue, signValue.length,
                            online);
            if (ret != 0) {
                LOGGER.info("OES_Verify failed, ret={}", ret);
            }

            return ret == 0;
        } finally {
            if (oes_api != null) {
                oes_api.release();
            }
        }
    }

    private static class OesSealObj {
        private byte[] seal;
        private int length;

        public byte[] getSeal() {
            return seal;
        }

        public int getLength() {
            return length;
        }

        public void setSeal(byte[] seal) {
            this.seal = seal;
        }

        public void setLength(int length) {
            this.length = length;
        }
    }

    public static class BAWrapper {
        private byte[] data;

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }
    }
}
