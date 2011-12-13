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

package org.saiku.adhoc.server.service.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.pentaho.reporting.engine.classic.core.AttributeNames;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportPreProcessor;
import org.pentaho.reporting.engine.classic.core.cache.CachingDataFactory;
import org.pentaho.reporting.engine.classic.core.function.ProcessingContext;
import org.pentaho.reporting.engine.classic.core.layout.output.DefaultProcessingContext;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
import org.pentaho.reporting.engine.classic.core.modules.parser.bundle.writer.BundleWriter;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.engine.classic.core.states.StateUtilities;
import org.pentaho.reporting.engine.classic.core.states.datarow.DefaultFlowController;
import org.pentaho.reporting.engine.classic.core.util.ReportParameterValues;
import org.pentaho.reporting.engine.classic.core.wizard.DataSchemaDefinition;
import org.pentaho.reporting.engine.classic.wizard.model.WizardSpecification;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.saiku.adhoc.exceptions.ModelException;
import org.saiku.adhoc.exceptions.ReportException;
import org.saiku.adhoc.model.dto.HtmlReport;
import org.saiku.adhoc.model.master.ReportTemplate;
import org.saiku.adhoc.model.master.SaikuMasterModel;
import org.saiku.adhoc.server.reporting.SaikuReportingComponent;
import org.saiku.adhoc.service.SaikuProperties;
import org.saiku.adhoc.service.report.ReportGeneratorService;
import org.saiku.adhoc.service.report.SaikuAdhocPreProcessor;
import org.saiku.adhoc.utils.ParamUtils;

public class ReportGeneratorServiceServer extends ReportGeneratorService {

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
    @Override
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

			String path =".";
			String action = model.getDerivedModels().getSessionId() + ".prpt";

			repository.writeLocalFile(path, action, prptContent.toByteArray());

			report.setData(stream.toString());

		} catch (Exception e) {
			throw new ReportException(e.getMessage());
		} finally {
			dataFactory.close();
		}

	}
	
	@Override
	protected void generateReport(MasterReport output, OutputStream stream,
            Map<String, Object> reportParameters, HtmlReport report, Integer acceptedPage) throws Exception {

        final SaikuReportingComponent reportComponent = new SaikuReportingComponent();
        reportComponent.setReport(output);
        reportComponent.setPaginateOutput(true);      
        reportComponent.setInputs(reportParameters);
        reportComponent.setDefaultOutputTarget(HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE);
        reportComponent.setOutputTarget(HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE);
        reportComponent.setDashboardMode(true);  
        reportComponent.setOutputStream(stream);
        reportComponent.setAcceptedPage(acceptedPage);  
    
       // reportComponent.validate();  
        reportComponent.execute();

        report.setCurrentPage(reportComponent.getAcceptedPage());
        report.setPageCount(reportComponent.getPageCount());
     

         
    }


}
