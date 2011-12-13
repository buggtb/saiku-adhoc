package org.saiku.adhoc.service.report.tasks;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.reporting.engine.classic.core.AttributeNames;
import org.pentaho.reporting.engine.classic.core.ReportElement;
import org.saiku.adhoc.model.master.SaikuColumn;
import org.saiku.adhoc.model.master.SaikuMasterModel;

public class SaikuUpdateFooterTask implements UpdateTask {
	
	private Log log = LogFactory.getLog(SaikuUpdateFooterTask.class);

	private List<SaikuColumn> columns;
	
	private SaikuMasterModel model;
	
	public SaikuUpdateFooterTask(SaikuMasterModel model) {
		this.columns = model.getColumns();
		this.model = model;
	}

	@Override
	public void processElement(ReportElement e, int index) {

		log.debug(e.getClass() + " " + e.getName());

		final String htmlClass = "saiku " + columns.get(index).getUid().replace("rpt-dtl-", "rpt-dtf-");
		
		e.setAttribute(AttributeNames.Html.NAMESPACE, AttributeNames.Html.STYLE_CLASS, htmlClass);


	}

}
