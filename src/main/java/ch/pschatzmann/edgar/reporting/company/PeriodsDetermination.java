package ch.pschatzmann.edgar.reporting.company;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import ch.pschatzmann.common.table.ITable;

/**
 * For an ITable we evaluate the reporting dates in order to identify the valid yearly K-10 dates,
 * The quarterly entries or the cumulated quarterly entries.
 * 
 * @author pschatzmann
 *
 */

public class PeriodsDetermination {
	private DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	private ITable<Object> table;
	private Integer weekForYearly = null;
	private List<Integer> weeksForQuarterly = null;
	private int weeksRange = 3;

	public PeriodsDetermination(ITable table) {
		this.table = table;
	}
	
	protected void setupYearly() {
		if (weekForYearly == null) {
			// remove entries where we reported quarterly values as part of the 10-K
			Map<Integer, Long> periodsCount = table.toList().stream()
					.filter(m -> ((String)m.get("form")).startsWith("10-K") && m.get("numberOfMonths").equals("12"))
					.map(m -> m.get("date").toString())
					.collect(Collectors.groupingBy(i -> getWeekOfYear(i), Collectors.counting()));

			// determine the value which was most often used
			if (!periodsCount.isEmpty()) {
				weekForYearly = getMaxCount(periodsCount).get(0).getKey();
			}
		}
	}
	
	protected void setupQuarterly() {
		if (weeksForQuarterly == null) {
			// remove entries where we reported quarterly values as part of the 10-K
			Map<Integer, Long> periodsCount = table.toList().stream()
					.filter(m ->  m.get("numberOfMonths").equals("3") && ((String)m.get("form")).startsWith("10-K"))
					.map(m -> m.get("date").toString())
					.collect(Collectors.groupingBy(i -> getWeekOfYear(i), Collectors.counting()));

			weeksForQuarterly = new ArrayList(periodsCount.keySet());			
			Collections.sort(weeksForQuarterly);
			for (Entry<Integer,Long> e : periodsCount.entrySet()) {
				weeksForQuarterly.remove((Integer)(e.getKey()+1));
			}
			
		}
	}

	protected List<Entry<Integer, Long>> getMaxCount(Map<Integer, Long> map) {
		return map.entrySet().stream()
				.sorted((o1, o2) -> -o1.getValue().compareTo(o2.getValue()))
				.collect(Collectors.toList());
	}

	protected boolean valueInRangeWeekOfYear(String date, int min, int max) {
		try {
			int value = getWeekOfYear(date);
			boolean result = value >= min && value <= max;
			return result;
		} catch (Exception ex) {
			return false;
		}
	}
	
	protected boolean valueInRange(String date, List<Integer> weeksForQuarterly2, int max) {
		for (int week : weeksForQuarterly2) {
			if (valueInRangeWeekOfYear(date,week-max,week+max)) {
				return true;
			}
		}
		return false;
	}


	protected int getWeekOfYear(String date) {
		try {
			GregorianCalendar calendar = new GregorianCalendar();
			calendar.setMinimalDaysInFirstWeek(7);
			calendar.setTime(df.parse(date));
			return calendar.get(GregorianCalendar.WEEK_OF_YEAR);
		} catch (Exception ex) {
			return -1;
		}
	}
	
	protected int getFirstQuarter() {
		int pos = weeksForQuarterly.indexOf(weekForYearly) + 1;
		return pos<weeksForQuarterly.size() ? weeksForQuarterly.get(pos) : weeksForQuarterly.get(0);
	}


	public boolean isValidYearly(String date, String form, String periods) {
		boolean result = false;
		if (!table.isEmpty()) {
			setupYearly();		
			result = form.equals("10-K") && periods.matches("12|0") && valueInRangeWeekOfYear(date, weekForYearly - weeksRange, weekForYearly + weeksRange);
		}
		return result;
	}

	public boolean isValidQuarterly(String date, String form, String periods) {
		boolean result = false;
		if (!table.isEmpty()) {
			setupQuarterly();
			result = periods.matches("3|0") && valueInRange(date, weeksForQuarterly, weeksRange);
		}
		return result;
	}

	public boolean isValidQuarterlyCumulated(String date, String form, String periods) {
		boolean result = false;
		if (!table.isEmpty()) {
			setupYearly();
			setupQuarterly();
			int weeksFor1stQuarter = getFirstQuarter();
			result = periods.matches("0|6|9|12") ||  periods.equals("3") && valueInRangeWeekOfYear(date, weeksFor1stQuarter - weeksRange, weeksFor1stQuarter + weeksRange);		
		}
		return result;
	}


}
