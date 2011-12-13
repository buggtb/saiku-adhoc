package org.saiku.adhoc.service.report.tasks;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.reporting.engine.classic.core.AttributeNames;
import org.pentaho.reporting.engine.classic.core.ReportElement;
import org.saiku.adhoc.model.master.SaikuColumn;
import org.saiku.adhoc.model.master.SaikuElementFormat;
import org.saiku.adhoc.model.master.SaikuMasterModel;
import org.saiku.adhoc.utils.TemplateUtils;

public class SaikuUpdateDetailsTask implements UpdateTask {
	
	private Log log = LogFactory.getLog(SaikuUpdateDetailsTask.class);

	private List<SaikuColumn> columns;

	private SaikuMasterModel model;

	public SaikuUpdateDetailsTask(SaikuMasterModel model) {
		this.columns = model.getColumns();
		this.model = model;
	}

	@Override
	public void processElement(ReportElement e, int index) {
		
		log.debug(e.getClass() + " " + e.getName());

		/*
		 * Markup for Client
		 */
		final SaikuColumn saikuColumn = columns.get(index);
	
		final String rptId = "rpt-dtl-" + index;
		
		//final String htmlClass = "saiku " + columns.get(index).getUid().replace("rpt-dtl-", "rpt-dth-");;
		
		final String htmlClass = "saiku " + rptId;
		
		model.getDerivedModels().getRptIdToSaikuElement().put(rptId, saikuColumn);
		
		e.setAttribute(AttributeNames.Html.NAMESPACE, AttributeNames.Html.STYLE_CLASS, htmlClass);
		
		e.setAttribute("http://reporting.pentaho.org/namespaces/engine/attributes/wizard", "allow-metadata-styling", Boolean.FALSE);

		
		/*
		 * Transfer element style
		 */	
		
		SaikuElementFormat tempFormat = (SaikuElementFormat) saikuColumn.getElementFormat().clone();
		
		TemplateUtils.mergeElementFormats(e.getStyle(), tempFormat);
		
		model.getDerivedModels().getRptIdToElementFormat().put(rptId, tempFormat);
		
	}

}
