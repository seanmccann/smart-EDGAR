package ch.pschatzmann.edgar.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import ch.pschatzmann.common.table.ITable;
import ch.pschatzmann.common.table.ITableEx;
import ch.pschatzmann.common.table.Value;
import ch.pschatzmann.common.utils.Utils;

/**
 * Creates a 2 dimensional table from a list of maps. The parameter names and
 * values are defined as map attributes
 * 
 * @author pschatzmann
 *
 */

public class ValueTable implements ITableEx<Value> {
	private static final long serialVersionUID = 1L;
	private List<String> keysToRemove;
	private String valueName;
	private String colName;
	private List<String> parameterNames;
	private List<String> rowFieldNames;
	private List<Key> rows = new ArrayList();
	private Map<CombinedKey, Value> valuesMap = new HashMap();

	/**
	 * Default Constructor
	 */
	public ValueTable(String valueName, String colName, List<String> keysToRemove) {
		this.keysToRemove = keysToRemove;
		this.valueName = valueName;
		this.colName = colName;
	}

	/**
	 * Adds a value for a since cell
	 * 
	 * @param maps
	 */
	public synchronized void addValues(Collection<Map<String, String>> maps) {
		setupRowKeyTitles(maps);	
		setupColumnTitles(maps);

		Set<Key> rows = new TreeSet();
		for (Map<String, String> record : maps) {
			Key row = this.getRowKey(record);
			Key col = this.getColKey(record.get(this.colName));
			Value value = new Value(record.get(this.valueName));
			rows.add(row);
			valuesMap.put(new CombinedKey(col, row), value);
		}
		
		if (this.rows != null) {
			rows.addAll(this.rows);
		} 
		this.rows = new ArrayList(rows);
	}

	private void setupRowKeyTitles(Collection<Map<String, String>> maps) {
		if (this.rowFieldNames==null) {
			Set<String> fields = maps.stream().map(m -> m.keySet()).flatMap(l -> l.stream()).collect(Collectors.toSet());
			fields.removeAll(this.keysToRemove);
			fields.remove(this.valueName);
			fields.remove(this.colName);
			this.rowFieldNames = new ArrayList(fields);
		}
	}

	private void setupColumnTitles(Collection<Map<String, String>> maps) {
		Set<String> colSet = maps.stream().map(m -> Utils.str(m.get(this.colName))).collect(Collectors.toSet());
		colSet = new TreeSet(colSet);
		if (this.parameterNames!=null)
			colSet.addAll(this.parameterNames);
		this.parameterNames = new ArrayList(colSet);
		Collections.sort(this.parameterNames);
	}
	
	

	/**
	 * Determines the row keys
	 * 
	 * @param record
	 * @return
	 */
	protected Key getRowKey(Map<String, String> record) {
		List<String> keyValues = this.getRowFieldNames().stream().map(field -> Utils.str(record.get(field)))
				.collect(Collectors.toList());
		Key rowKey = new Key(keyValues);
		return rowKey;
	}

	protected Key getColKey(String parameterName) {
		Key rowKey = new Key(Arrays.asList(parameterName));
		return rowKey;
	}

	@Override
	public List<String> getRowFieldNames() {
		return this.rowFieldNames;
	}

	@Override
	public int getColumnCount() {
		return this.parameterNames.size();
	}

	@Override
	public String getColumnTitle(int col) {
		return this.parameterNames.get(col);
	}

	@Override
	public int getRowCount() {
		return rows.size();
	}

	@Override
	public List<String> getRowValue(int row) {
		return rows.get(row).getKeyValues();
	}

	@Override
	public Value getValue(int col, int row) {
		Key rowKey = rows.get(row);
		Key colKey = this.getColKey(parameterNames.get(col));
		return valuesMap.get(new CombinedKey(colKey, rowKey));
	}

	@Override
	public ITable getBaseTable() {
		return this;
	}

	@Override
	public ITableEx<Value> addColumnKey(String parameterName) {
		if (!parameterNames.contains(parameterName))
			this.parameterNames.add(parameterName);
		return this;
	}

}
