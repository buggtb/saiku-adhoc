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

package org.saiku.adhoc.service;

import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.engine.services.solution.SimpleContentGenerator;

/**
 * @author mgiepz
 *
 */
public class SaikuAdhocContentGenerator extends SimpleContentGenerator {

	private static final long serialVersionUID = -9180003935693305152L;
	private static final Log logger = LogFactory
			.getLog(SaikuAdhocContentGenerator.class);
	
	public String getMimeType() {
		return "text/html";
	}

	public Log getLogger() {
		return logger;
	}

	@Override
	public void createContent(OutputStream out) throws Exception {

	}


}