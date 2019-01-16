package ch.pschatzmann.edgar.reporting.company;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Selection Criteria for the selection of a company
 * 
 * @author pschatzmann
 *
 */
public class CompanySelection implements Serializable {
	private static final long serialVersionUID = 1L;
	private String cik = "";
	private String companyName = "";
	private String tradingSymbol = "";

	public String getCompanyNumber() {
		return cik;
	}

	public CompanySelection setCompanyNumber(String cik) {
		this.cik = cik != null ? cik : "";
		return this;
	}

	public String getCompanyName() {
		return companyName;
	}

	public CompanySelection setCompanyName(String companyName) {
		this.companyName = companyName != null ? companyName : "";
		return this;
	}

	public String getTradingSymbol() {
		return tradingSymbol;
	}

	public CompanySelection setTradingSymbol(String tradingSymbol) {
		this.tradingSymbol = tradingSymbol !=null ? tradingSymbol : "";
		return this;
	}

	@JsonIgnore
	public boolean isValid() {
		return !this.cik.isEmpty() || !this.companyName.isEmpty() || !this.tradingSymbol.isEmpty();
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("id:");
		sb.append(this.getCompanyNumber());
		sb.append(",symbol:");
		sb.append(this.getTradingSymbol());
		sb.append(",");
		sb.append(this.getCompanyName());
		return sb.toString();
		
	}

}
