package org.saiku.adhoc.service.report.tasks;

import org.pentaho.reporting.engine.classic.core.AttributeNames;
import org.pentaho.reporting.engine.classic.core.ReportElement;
import org.saiku.adhoc.model.master.SaikuColumn;
import org.saiku.adhoc.model.master.SaikuGroup;
import org.saiku.adhoc.utils.TemplateUtils;

public class SaikuUpdateGroupHeaderTask implements UpdateTask {
	
	private SaikuGroup groupDefinition;
	private int groupIndex;

	public SaikuUpdateGroupHeaderTask(SaikuGroup saikuGroup, int groupIndex) {
		this.groupDefinition = saikuGroup;
		this.groupIndex = groupIndex;
	}

	@Override
	public void processElement(ReportElement e, int index) {
		
		/* TODO Auto-generated method stu
		 *
		 * Markup for Client
		 */
		
		final String htmlClass = "saiku " + groupDefinition.getUid();
		
		e.setAttribute(AttributeNames.Html.NAMESPACE, AttributeNames.Html.STYLE_CLASS, htmlClass);
	
		e.setAttribute("http://reporting.pentaho.org/namespaces/engine/attributes/wizard", "allow-metadata-styling", Boolean.FALSE);

		/*
		 * Transfer element style
		 */	
		//TemplateUtils.mergeElementFormats(e.getStyle());

	}

}
