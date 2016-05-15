package cn.yiiguxing.plugin.translate;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

public class QueryResult {

	public static final int ERROR_CODE_NONE = 0;
	public static final int ERROR_CODE_QUERY_TOO_LONG = 20;
	public static final int ERROR_CODE_FAIL = 30;
	public static final int ERROR_CODE_UNSUPPORTED_LANG = 40;
	public static final int ERROR_CODE_INVALID_KEY = 50;
	public static final int ERROR_CODE_NO_RESULT = 60;

	@SerializedName(value = "query")
	private String query;
	@SerializedName(value = "errorCode")
	private int errorCode;
	@SerializedName(value = "translation")
	private String[] translation;

	private BasicExplain basicExplain;
	private WebExplain[] webExplains;

	public QueryResult() {
	}

	public QueryResult(String query) {
		this.query = query;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String[] getTranslation() {
		return translation;
	}

	public void setTranslation(String[] translation) {
		this.translation = translation;
	}

	public BasicExplain getBasicExplain() {
		return basicExplain;
	}

	public void setBasicExplain(BasicExplain basicExplain) {
		this.basicExplain = basicExplain;
	}

	public WebExplain[] getWebExplains() {
		return webExplains;
	}

	public void setWebExplains(WebExplain[] webExplains) {
		this.webExplains = webExplains;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + errorCode;
		result = prime * result + ((query == null) ? 0 : query.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueryResult other = (QueryResult) obj;
		if (errorCode != other.errorCode)
			return false;
		if (query == null) {
			if (other.query != null)
				return false;
		} else if (!query.equals(other.query))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "QueryResult [query=" + query + ", errorCode=" + errorCode
				+ ", translation=" + Arrays.toString(translation)
				+ ", basicExplain=" + basicExplain + ", webExplains="
				+ Arrays.toString(webExplains) + "]";
	}

}
