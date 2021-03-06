package ch.pschatzmann.edgar.table.forecast;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.log4j.Logger;

import com.workday.insights.timeseries.arima.Arima;
import com.workday.insights.timeseries.arima.struct.ArimaParams;
import com.workday.insights.timeseries.arima.struct.ForecastResult;

import ch.pschatzmann.common.table.IForecast;
import ch.pschatzmann.common.table.TableCalculated;

/**
 * Calculates the forecasted value for the indicated row.
 * We calculate the predicted value with the help of ARIMA
 * 
 * @author pschatzmann
 *
 */
public class ForecastQuartersCumulatedARIMA implements IForecast {
	private static final Logger LOG = Logger.getLogger(ForecastQuartersCumulatedARIMA.class);
	private TableCalculated table;
	private List<String> parameterNames;
	private ArimaParams params = null;

	public ForecastQuartersCumulatedARIMA() {	
		this(new ArimaParams(3, 0, 3, 1, 1, 0, 0));
	}

	public ForecastQuartersCumulatedARIMA(ArimaParams params) {
		this.params = params;
	}

	public Double forecast(String parameter, int row) {
		Double result = null;
		if (row>1) {
			// get history
			List<ForecastValue> past = getHistory(parameter, row-1);
			ForecastValue last = past.get(past.size()-1);

			double[] values = past.stream().map(v -> v.value).mapToDouble(d ->d).toArray();	
			ForecastResult forcast = Arima.forecast_arima(values, 1, params);
			double[] forecastedValues = forcast.getForecast();

			Double arimaEstimate = forecastedValues[0];
			// Calculate cumulated result with predicted value
			result = predictCumulated(last, arimaEstimate);	
		}
		
		return result;
	}

	protected List<ForecastValue> getHistory(String parameterName, int toRow) {
		int col = this.getParameterNames().indexOf(parameterName);
		List<ForecastValue> values = new ArrayList();
		// history
		for (int row = 0; row <= toRow; row++) {
			Number value = table.eval(row, table.getColumnTitle(col));
			values.add(setValue(new ForecastValue(row),values, value));
		}
		return values;
	}
	
	protected List<String> getParameterNames() {
		if (this.parameterNames==null) {
			this.parameterNames = IntStream.range(0, table.getColumnCount())
				    .mapToObj(col -> table.getColumnTitle(col))
				    .collect(Collectors.toList());
		}
		return this.parameterNames;
	}

	
	protected ForecastValue setValue(ForecastValue v,List<ForecastValue> list, Number valueNumber){
		if (!list.isEmpty()) {
			v.prior = list.get(v.index-1);
			if (valueNumber!=null) {
				try {
					v.valueCumulated = valueNumber.doubleValue();
					if (v.index>=1 && v.valueCumulated  > list.get(v.index-1).valueCumulated) {
						v.seasonIndex = v.prior.seasonIndex + 1;
						v.value = v.valueCumulated - v.prior.valueCumulated;
					} else {
						v.seasonIndex = 0;
						v.value = v.valueCumulated;
					}
				} catch(Exception ex) {
					LOG.warn(ex,ex);;
				}
			} else {
				v.seasonIndex = (v.prior.seasonIndex + 1);
				if (v.seasonIndex==4) v.seasonIndex = 0;
			}
		}
		return v;		
	}
	
	protected Double predictCumulated(ForecastValue last, Double current) {
		Double result = null;
		if (last!=null) {
			if (current !=null && Double.isFinite(current)) {
				result = last.seasonIndex==3 ? current : last.valueCumulated+current;
			}
		}
		return result;
	}

	protected double linearRegressionFor(int row, List<Double> past) {
		double regressionResult = 0;
		if (past.size()>1) {
			SimpleRegression regression = new SimpleRegression();
			for (int j=0;j<past.size();j++) {
				regression.addData(j, past.get(j));
			}
			regressionResult = regression.predict(row);
		} else if (past.size()==1){
			regressionResult = past.get(0);
		}
		return regressionResult;
	}

	@Override
	public void setTable(TableCalculated table) {
		this.table = table;		
	}	
	
}
