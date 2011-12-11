package org.saiku.adhoc.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.saiku.adhoc.model.master.SaikuMasterModel;
import org.saiku.adhoc.model.master.SaikuParameter;

public class ParamUtils {

	public static Map<String, Object> getReportParameters(String prefix,SaikuMasterModel model) {

		Map<String, Object> reportParameters = new HashMap<String, Object>();

		SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

		final ArrayList<SaikuParameter> parameters = model.getParameters();
		for (SaikuParameter saikuParameter : parameters) {

			final String categoryId = saikuParameter.getCategory();
			final String columnId = saikuParameter.getId();
			final String parameterName = prefix + "F_" + categoryId + "_" + columnId;

			if (saikuParameter.getType().equals("String")) {
				ArrayList<String> valueList = saikuParameter
				.getParameterValues();
				String[] values = valueList
				.toArray(new String[valueList.size()]);
				reportParameters.put(parameterName, values);
			}
			if (saikuParameter.getType().equals("Date")) {
				String nameFrom = parameterName + "_FROM";
				String nameTo = parameterName + "_TO";
				ArrayList<String> valueList = saikuParameter
				.getParameterValues();
				String[] values = valueList
				.toArray(new String[valueList.size()]);

				reportParameters.put(nameFrom, values[0]);
				reportParameters.put(nameTo, values[1]);

				
				try {
					reportParameters.put(nameFrom, dateFormat.parse(values[0]));
					reportParameters.put(nameTo, dateFormat.parse(values[1]));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		}
		return reportParameters;
	}
	
	public static String[] splitFirst(String source, String splitter)
	  {
	    // hold the results as we find them
	    Vector rv = new Vector();
	    int last = 0;
	    int next = 0;

	    // find first splitter in source
	    next = source.indexOf(splitter, last);
	    if (next != -1)
	    {
	      // isolate from last thru before next
	      rv.add(source.substring(last, next));
	      last = next + splitter.length();
	    }

	    if (last < source.length())
	    {
	      rv.add(source.substring(last, source.length()));
	    }

	    // convert to array
	    return (String[]) rv.toArray(new String[rv.size()]);
	  }


}
