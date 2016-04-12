package com.bwts.oestest.dto;

public class OESSeal {
    private String id;
    private String version;
    private String venderId;
    private String type;
    private String name;
    private String certInfo;
    private byte[] validStart;
    private byte[] validEnd;
    private byte[] signDate;
    private String signerName;
    private String signMethod;

    public OESSeal() {
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVenderId() {
        return this.venderId;
    }

    public void setVenderId(String venderId) {
        this.venderId = venderId;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCertInfo() {
        return this.certInfo;
    }

    public void setCertInfo(String certInfo) {
        this.certInfo = certInfo;
    }

    public byte[] getValidStart() {
        return validStart;
    }

    public void setValidStart(byte[] validStart) {
        this.validStart = validStart;
    }

    public byte[] getValidEnd() {
        return validEnd;
    }

    public void setValidEnd(byte[] validEnd) {
        this.validEnd = validEnd;
    }

    public byte[] getSignDate() {
        return signDate;
    }

    public void setSignDate(byte[] signDate) {
        this.signDate = signDate;
    }

    public String getSignerName() {
        return this.signerName;
    }

    public void setSignerName(String signerName) {
        this.signerName = signerName;
    }

    public String getSignMethod() {
        return this.signMethod;
    }

    public void setSignMethod(String signMethod) {
        this.signMethod = signMethod;
    }
}
