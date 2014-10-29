package com.plugin.internet.impl;


import com.plugin.internet.annotations.response.JsonCreator;
import com.plugin.internet.annotations.response.JsonProperty;
import com.plugin.internet.interfaces.ResponseBase;

public class JsonErrorResponse extends ResponseBase {

	public int errorCode;

	public String errorMsg;

	@JsonCreator
	public JsonErrorResponse(@JsonProperty("code") int code,
			@JsonProperty("data") String data) {
		this.errorCode = code;
		this.errorMsg = data;
	}
}
