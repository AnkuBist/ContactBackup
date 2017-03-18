package com.hgil.contactbackup.retrofit.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class defaultResponse {

    @SerializedName("returnCode")
    @Expose
    private Boolean returnCode;
    @SerializedName("strMessage")
    @Expose
    private String strMessage;

    public Boolean getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(Boolean returnCode) {
        this.returnCode = returnCode;
    }

    public String getStrMessage() {
        return strMessage;
    }

    public void setStrMessage(String strMessage) {
        this.strMessage = strMessage;
    }

}