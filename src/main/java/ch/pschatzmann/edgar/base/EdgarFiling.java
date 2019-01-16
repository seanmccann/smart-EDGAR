package ch.pschatzmann.edgar.base;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import ch.pschatzmann.common.utils.Tuple;
import ch.pschatzmann.common.utils.Utils;
import ch.pschatzmann.edgar.base.Fact.DataType;
import ch.pschatzmann.edgar.service.EdgarFileService;

/**
 * Simple API to provide access to File filings 
 * 
 * @author pschatzmann
 *
 */
public class EdgarFiling {
	private String fileName;
	private String form;
	private XBRL xbrl;

	public EdgarFiling(String filing) {
		this.fileName = filing;
		this.setupForm();
	}
	
	/**
	 * Returns the file of the filing
	 * @return
	 */
	public File getFile() {
		return EdgarFileService.getFile(fileName);
	}

	/**
	 * Finds all filings which exist in the file system
	 * @param regex
	 * @return
	 * @throws Exception 
	 */
	public static List<EdgarFiling> list(String regex) throws Exception {
		return EdgarFileService.getCompanies().stream()
		   .map(c -> new EdgarFileService().getFilings(c).stream())
		   .flatMap(l -> l)
		   .filter(f -> f.matches(regex))
		   .map(f -> new EdgarFiling(f))
		   .collect(Collectors.toList());
		
	}
	
	/**
	 * Loads the indicated filings into one combined XBRL 
	 * @param filings
	 * @return
	 */
	public static XBRL getXBRL(Collection<EdgarFiling> filings) {
		XBRL xbrl = new XBRL();
		xbrl.setConvertHtmlToText(true);
		filings.forEach(f -> xbrl.tryLoad(f.getFile(),null));
		return xbrl;
	}


	/**
	 * Determines the XBRL
	 * @return
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public XBRL getXBRL() throws SAXException, IOException, ParserConfigurationException {
		if (xbrl==null) {
			xbrl = new XBRL();
			xbrl.setConvertHtmlToText(true);
			xbrl.load(EdgarFileService.getFile(fileName));
		}
		return xbrl;
	}
	

	/**
	 * Returns the entries of the zip reporting file
	 * @return
	 * @throws IOException 
	 * @throws ZipException 
	 */
	public List<String> getEntries() throws ZipException, IOException{
		ZipFile zipFile = new ZipFile(getFile());
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        List<String> result = new ArrayList();;
        while(entries.hasMoreElements()) {
        	ZipEntry entry = entries.nextElement();
        	result.add(entry.getName());
        }
        return result;		
	}
	
	protected void setupForm() {
		int start = this.fileName.indexOf("-");
		if (start>=0) {
			String result = this.fileName.substring(start+1);
			int end = result.lastIndexOf("-");
			if (end>=0) {
				String form =  result.substring(0,end);
				if (form.matches("10-K.*|10-Q.*")) {
					this.form = form;
				}
			}
		}
	}

	/**
	 * Provides the CompanyInfo for the filing
	 * @return
	 */
	public EdgarCompany getCompanyInfo() {
		int end = this.fileName.indexOf("-");
		return new EdgarCompany(fileName.substring(0, end));
	}

	/**
	 * Provides the date as yyyy-MM-dd
	 * @return
	 */
	public String getDate() {
		int start = this.fileName.lastIndexOf("-");
		String date = fileName.substring(start+1);
		StringBuffer sb = new StringBuffer();
		sb.append(date.substring(0,4));
		sb.append("-");
		sb.append(date.substring(4,6));
		sb.append("-");
		sb.append(date.substring(6));
		return sb.toString();
	}
	
	/**
	 * Provides the filing id
	 * @return
	 */
	public String getFileName() {
		return this.fileName;
	}
	
	@Override
	public String toString() {
		return this.fileName;
	}

	/**
	 * Defines the form
	 * @param form
	 */
	public void setForm(String form) {
		this.form = form;
	}
	
	/**
	 * Determines the form
	 * @return
	 */
	public String getForm() {
		return this.form;
	}

}
