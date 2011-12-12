/*
 * Copyright (C) 2011 Marius Giepz
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the Free 
 * Software Foundation; either version 2 of the License, or (at your option) 
 * any later version.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 *
 */

package org.saiku.adhoc.service.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.reporting.engine.classic.core.AbstractReportDefinition;
import org.pentaho.reporting.engine.classic.core.AttributeNames;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportPreProcessor;
import org.pentaho.reporting.engine.classic.core.cache.CachingDataFactory;
import org.pentaho.reporting.engine.classic.core.function.ProcessingContext;
import org.pentaho.reporting.engine.classic.core.function.StructureFunction;
import org.pentaho.reporting.engine.classic.core.layout.output.DefaultProcessingContext;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
import org.pentaho.reporting.engine.classic.core.modules.parser.bundle.writer.BundleWriter;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.engine.classic.core.states.StateUtilities;
import org.pentaho.reporting.engine.classic.core.states.datarow.DefaultFlowController;
import org.pentaho.reporting.engine.classic.core.util.ReportParameterValues;
import org.pentaho.reporting.engine.classic.core.wizard.DataSchemaDefinition;
import org.pentaho.reporting.engine.classic.wizard.WizardOverrideFormattingFunction;
import org.pentaho.reporting.engine.classic.wizard.WizardProcessor;
import org.pentaho.reporting.engine.classic.wizard.model.WizardSpecification;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.saiku.adhoc.exceptions.ModelException;
import org.saiku.adhoc.exceptions.ReportException;
import org.saiku.adhoc.model.WorkspaceSessionHolder;
import org.saiku.adhoc.model.dto.HtmlReport;
import org.saiku.adhoc.model.master.ReportTemplate;
import org.saiku.adhoc.model.master.SaikuMasterModel;
import org.saiku.adhoc.model.master.SaikuParameter;
import org.saiku.adhoc.service.SaikuProperties;
import org.saiku.adhoc.service.repository.IRepositoryHelper;
import org.saiku.adhoc.utils.ParamUtils;

public class ReportGeneratorService {

	private WorkspaceSessionHolder sessionHolder;

	private IRepositoryHelper repository;

	private static final Log log = LogFactory
	.getLog(ReportGeneratorService.class);

	public void setSessionHolder(WorkspaceSessionHolder sessionHolder) {
		this.sessionHolder = sessionHolder;
	}

	public void setRepositoryHelper(IRepositoryHelper repository) {
		this.repository = repository;
	}

	/**
	 * Renders the report for a given query to html
	 * 
	 * @param sessionId
	 * @param report 
	 * @param acceptedPage 
	 * @param template 
	 * @return
	 * @throws ReportException
	 * @throws ModelException
	 * @throws IOException
	 * @throws ResourceException
	 */
	public void renderReportHtml(String sessionId, String templateName, HtmlReport report, Integer acceptedPage) throws ReportException,
	ModelException, ResourceException {

		SaikuMasterModel model = sessionHolder.getModel(sessionId);

		templateName = templateName.equals("default")? SaikuProperties.defaultPrptTemplate : templateName + ".prpt";
		
		ReportTemplate template =  new ReportTemplate("system", "saiku-adhoc/resources/templates", templateName);
		
		model.setReportTemplate(template);
		
		CachingDataFactory dataFactory = null;

		if (log.isDebugEnabled()) {
			log.debug("SERVICE:ReportGeneratorService " + sessionId + " renderReportHtml\t" + sessionHolder.logModel(sessionId));
		}

		model.deriveModels();

		final MasterReport reportTemplate = model.getDerivedModels().getReportTemplate();

		try {

			final WizardSpecification wizardSpecification = model
			.getWizardSpecification();

			reportTemplate.setDataFactory(model.getDerivedModels()
					.getCdaDataFactory());
			reportTemplate.setQuery(model.getDerivedModels().getSessionId());

			reportTemplate.setAttribute(AttributeNames.Wizard.NAMESPACE,
					"wizard-spec", wizardSpecification);



			final ProcessingContext processingContext = new DefaultProcessingContext();
			final DataSchemaDefinition definition = reportTemplate
			.getDataSchemaDefinition();

			//-----------------------------------
			
			final ReportParameterValues parameterValues = StateUtilities.computeParameterValueSet(reportTemplate,
					getReportParameterValues(model));
			
			
			
			final ParameterDefinitionEntry[] parameterDefinitions = reportTemplate.getParameterDefinition()
			.getParameterDefinitions();
			
			for (int i = 0; i < parameterDefinitions.length; i++) {
				ParameterDefinitionEntry p = parameterDefinitions[i];
				log.debug(">>> " + p.getName() + " " + p.getValueType());
			}
			
			final String[] columnNames = parameterValues.getColumnNames();
			
			for (int i = 0; i < columnNames.length; i++) {
				log.debug(">>> " + columnNames[i] + " = " + parameterValues.get(columnNames[i]).toString());
			}
			
			//-----------------------------------			
			
			final DefaultFlowController flowController = new DefaultFlowController(
					processingContext, definition,
					parameterValues,
							parameterDefinitions, false);

			ensureSaikuPreProcessorIsAdded(reportTemplate, model);
			ensureHasOverrideWizardFormatting(reportTemplate, flowController);

			dataFactory = new CachingDataFactory(
					reportTemplate.getDataFactory(), false);
			dataFactory.initialize(processingContext.getConfiguration(),
					processingContext.getResourceManager(),
					processingContext.getContentBase(),
					processingContext.getResourceBundleFactory());

			//final WizardProcessor processor = new WizardProcessor();

			final DefaultFlowController postQueryFlowController = flowController
			.performQuery(dataFactory, reportTemplate.getQuery(),
					reportTemplate.getQueryLimit(), reportTemplate
					.getQueryTimeout(), flowController
					.getMasterRow().getResourceBundleFactory());

			// final Object originalEnable =
			// reportTemplate.getAttribute(AttributeNames.Wizard.NAMESPACE,
			// AttributeNames.Wizard.ENABLE);
			//
			// reportTemplate.setAttribute(AttributeNames.Wizard.NAMESPACE,
			// AttributeNames.Wizard.ENABLE, Boolean.TRUE);
			// final MasterReport preOutput =
			// processor.performPreProcessing(reportTemplate,
			// postQueryFlowController);
			// preOutput.setAttribute(AttributeNames.Wizard.NAMESPACE,
			// AttributeNames.Wizard.ENABLE, originalEnable);
			//
			// final MasterReport output =
			// processor.performPreProcessing(preOutput,
			// postQueryFlowController);

			final Object originalEnable = reportTemplate.getAttribute(
					AttributeNames.Wizard.NAMESPACE,
					AttributeNames.Wizard.ENABLE);
			reportTemplate.setAttribute(AttributeNames.Wizard.NAMESPACE,
					AttributeNames.Wizard.ENABLE, Boolean.TRUE);

			
			//
//			
			ReportPreProcessor processor = new SaikuAdhocPreProcessor();
			((SaikuAdhocPreProcessor) processor).setSaikuMasterModel(model);
			final MasterReport output = processor.performPreProcessing(
					reportTemplate, postQueryFlowController);
			
			//final MasterReport output = processor.performPreProcessing(
			//		reportTemplate, flowController);
			output.setAttribute(AttributeNames.Wizard.NAMESPACE,
					AttributeNames.Wizard.ENABLE, Boolean.FALSE);	
			
			
//			final MasterReport output = reportTemplate;
			
			// output.setAttribute(AttributeNames.Wizard.NAMESPACE,
			// AttributeNames.Wizard.ENABLE, originalEnable);

			ByteArrayOutputStream stream = new ByteArrayOutputStream();

//			ensureSaikuPreProcessorIsRemoved(output);
//			
//			final ByteArrayOutputStream prptContent1 = new ByteArrayOutputStream();
//			BundleWriter.writeReportToZipStream(output, prptContent1);
//			String solutiona = "bi-developers";
//			String patha = "/cda";
//			String actiona = model.getDerivedModels().getSessionId() + "-pre.prpt";
//			repository.writeFile(solutiona, patha, actiona, prptContent1);
	
			generateReport(output, stream, ParamUtils.getReportParameters("", model), report, acceptedPage);

			ensureSaikuPreProcessorIsRemoved(output);
		
			final ByteArrayOutputStream prptContent = new ByteArrayOutputStream();
			BundleWriter.writeReportToZipStream(output, prptContent);
			//TODO FIX PLUGIN
			//String solution = "system";
			//String path = "saiku-adhoc/temp";
			String path =".";
			String action = model.getDerivedModels().getSessionId() + ".prpt";
//			repository.writeFile(solution, path, action, prptContent);
			repository.writeLocalFile(path, action, prptContent.toByteArray());

			report.setData(stream.toString());

		} catch (Exception e) {
			throw new ReportException(e.getMessage());
		} finally {
			dataFactory.close();
		}

	}

	private ReportParameterValues getReportParameterValues(
			SaikuMasterModel model) {

		ReportParameterValues vals = new ReportParameterValues();

		Map<String, Object> reportParameters = ParamUtils.getReportParameters("", model);

		if (null != model) {
			Collection<String> keyset = reportParameters.keySet();
			for (Iterator<String> iterator = keyset.iterator(); iterator
			.hasNext();) {
				String key = (String) iterator.next();
				vals.put(key, reportParameters.get(key));
			}
		}
		return vals;
	}

	private Map<String, Object> getReportParameters(SaikuMasterModel model) {

		Map<String, Object> reportParameters = new HashMap<String, Object>();
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
		
		final ArrayList<SaikuParameter> parameters = model.getParameters();
		for (SaikuParameter saikuParameter : parameters) {

			final String categoryId = saikuParameter.getCategory();
			final String columnId = saikuParameter.getId();	
			final String parameterName = "F_" + categoryId + "_" + columnId;
			
			if(saikuParameter.getType().equals("String")){
				ArrayList<String> valueList = saikuParameter.getParameterValues();
				String[] values = valueList.toArray(new String[valueList.size()]);
				reportParameters.put(parameterName, values);
			}
			if(saikuParameter.getType().equals("Date")){
				String nameFrom = parameterName + "_FROM";
				String nameTo = parameterName + "_TO";
				ArrayList<String> valueList = saikuParameter.getParameterValues();
				String[] values = valueList.toArray(new String[valueList.size()]);
				
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

	/**
	 * Generate the report
	 * 
	 * @param output
	 * @param stream
	 * @param report 
	 * @param acceptedPage 
	 * @param query2
	 * @throws Exception 
	 */
	private void generateReport(MasterReport output, OutputStream stream,
			Map<String, Object> reportParameters, HtmlReport report, Integer acceptedPage) throws Exception {

	    final SimpleReportingComponent reportComponent = new SimpleReportingComponent();
	    reportComponent.setReport(output);
	    reportComponent.setPaginateOutput(true);      
		reportComponent.setInputs(reportParameters);
	    reportComponent.setDefaultOutputTarget(HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE);
	    reportComponent.setOutputTarget(HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE);
	    reportComponent.setDashboardMode(true);  
	    reportComponent.setOutputStream(stream);
	    reportComponent.setAcceptedPage(acceptedPage);  
	
	    reportComponent.validate();  
	    reportComponent.execute();

	    report.setCurrentPage(reportComponent.getAcceptedPage());
	    report.setPageCount(reportComponent.getPageCount());
	 
	}
	

	private void ensureSaikuPreProcessorIsRemoved(final AbstractReportDefinition element) {

		final ReportPreProcessor[] oldProcessors = element.getPreProcessors();
		
		ArrayList<ReportPreProcessor> newProcessors = new ArrayList<ReportPreProcessor>();

		for (int i = 0; i < oldProcessors.length; i++)
		{
			ReportPreProcessor processor = oldProcessors[i];
			if (!(processor instanceof SaikuAdhocPreProcessor  || processor instanceof WizardProcessor))
			{
				newProcessors.add(processor);
			}
		}
		
		final ReportPreProcessor[] array = newProcessors.toArray(new ReportPreProcessor[newProcessors.size()]);
		element.setAttribute(AttributeNames.Internal.NAMESPACE, AttributeNames.Internal.PREPROCESSORS, array);

	}


	private static void ensureSaikuPreProcessorIsAdded(final AbstractReportDefinition element,
			SaikuMasterModel model)
	{
		final ReportPreProcessor[] processors = element.getPreProcessors();
		boolean hasSaikuProcessor = false;
		for (int i = 0; i < processors.length; i++)
		{
			final ReportPreProcessor processor = processors[i];
			if (processor instanceof SaikuAdhocPreProcessor)
			{
				hasSaikuProcessor = true;
				//Set the model on the processor
				((SaikuAdhocPreProcessor) processor).setSaikuMasterModel(model);

			}
		}
		if (!hasSaikuProcessor)
		{
			//Add a new processor with the current model
			final SaikuAdhocPreProcessor processor = new SaikuAdhocPreProcessor();
			processor.setSaikuMasterModel(model);
			element.addPreProcessor(processor);
		}
	}
	
	private static void ensureHasOverrideWizardFormatting(
			AbstractReportDefinition reportTemplate, 
			DefaultFlowController flowController) {
		
		final StructureFunction[] structureFunctions = reportTemplate.getStructureFunctions();
		
		boolean hasOverrideWizardFormatting = false;
		
		for (int i = 0; i < structureFunctions.length; i++) {
			final StructureFunction structureFunction = structureFunctions[i];
			if(structureFunction instanceof WizardOverrideFormattingFunction){
				hasOverrideWizardFormatting = false;
				break;
			}
		}
		if(!hasOverrideWizardFormatting){
			reportTemplate.addStructureFunction(new WizardOverrideFormattingFunction());
		}
				
		
	}
	 

}
