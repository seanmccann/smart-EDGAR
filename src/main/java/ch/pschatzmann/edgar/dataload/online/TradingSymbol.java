package ch.pschatzmann.edgar.dataload.online;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ch.pschatzmann.edgar.dataload.DownloadProcessorJDBC;
import ch.pschatzmann.edgar.dataload.rss.FeedInfoRecord;
import ch.pschatzmann.edgar.dataload.rss.RSSDataSource;
import ch.pschatzmann.edgar.utils.Utils;

/**
 * Trading Symbol determination from EDGAR ownership filings
 * 
 * @author pschatzmann
 *
 */
public class TradingSymbol implements Serializable {
	private static final Logger LOG = Logger.getLogger(DownloadProcessorJDBC.class);
	private String companyID;
	private java.util.List<String> invalidTickers = Arrays.asList("NONE","N/A","NA","TBD","[NONE]");
	private String tradingSymbol;

	public TradingSymbol(String companyID) {
		this.companyID = companyID;
	}

	/**
	 * Find the first ownership document for each company
	 * 
	 * @return
	 */
	public String getTradingSymbol()  {
		try {
			if (tradingSymbol == null) {
				String url = "https://www.sec.gov/cgi-bin/browse-edgar?action=getcompany&CIK=%cik&type=&dateb=&owner=only&start=0&count=1&output=atom";
				url = url.replaceAll("%cik", this.companyID);
		
				for (FeedInfoRecord feedInfo : RSSDataSource.createForUrl(url).getData(".*")) {
					org.jsoup.nodes.Document doc = Jsoup.parse(new URL(feedInfo.getUrlHttp()), 10000);
					Elements elements = doc.getElementsMatchingOwnText(".\\.xml");
					if (elements!=null) {
						if (!elements.isEmpty()) {
							try {
								String xml = elements.first().attr("href");
								String result = getTradinySymbol(new URL("https://www.sec.gov" + xml).openStream());
								if (!Utils.isEmpty(result)) {
									result = result.toUpperCase();
									tradingSymbol = invalidTickers.contains(result) ? "":result;
									break;
								}
							} catch (Exception ex) {
								LOG.error(ex, ex);
							}
						}
					}
				}
			
			}
			return tradingSymbol;
		} catch(Exception ex) {
			LOG.warn("Could not determine Trading Symobl",ex);
		}
		return "";
	}

	/**
	 * Extract the trading symbol from the XML document
	 * 
	 * @param is
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws XPathExpressionException
	 */
	protected static String getTradinySymbol(InputStream is)
			throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(is);
		doc.getDocumentElement().normalize();
		XPath xPath = XPathFactory.newInstance().newXPath();
		String result = (String) xPath.compile("/ownershipDocument/issuer/issuerTradingSymbol").evaluate(doc,
				XPathConstants.STRING);
		return result;
	}

}
