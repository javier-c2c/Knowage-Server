/*
 * Knowage, Open Source Business Intelligence suite
 * Copyright (C) 2016 Engineering Ingegneria Informatica S.p.A.
 *
 * Knowage is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Knowage is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.eng.spagobi.engines.qbe.services.core.catalogue;

import it.eng.spago.base.SourceBean;
import it.eng.spago.error.EMFAbstractError;
import it.eng.spago.error.EMFErrorHandler;
import it.eng.spago.error.EMFErrorSeverity;
import it.eng.spagobi.commons.QbeEngineStaticVariables;
import it.eng.spagobi.engines.qbe.services.core.AbstractQbeEngineAction;
import it.eng.spagobi.utilities.assertion.Assert;
import it.eng.spagobi.utilities.engines.EngineAnalysisMetadata;
import it.eng.spagobi.utilities.engines.SpagoBIEngineServiceException;
import it.eng.spagobi.utilities.engines.SpagoBIEngineServiceExceptionHandler;
import it.eng.spagobi.utilities.service.JSONSuccess;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * This action is responsible to persist the queries contained into the catalogue
 */
public class SaveAnalysisStateAction extends AbstractQbeEngineAction {

	private static final long serialVersionUID = -2692347943059370260L;
	public static final String SERVICE_NAME = "SAVE_ANALYSIS_STATE_ACTION";

	@Override
	public String getActionName() {
		return SERVICE_NAME;
	}

	//

	/** Logger component. */
	public static transient Logger logger = Logger.getLogger(SaveAnalysisStateAction.class);

	@Override
	public void service(SourceBean request, SourceBean response) {

		String queryName = null;
		String queryDescritpion = null;
		String queryScope = null;
		EngineAnalysisMetadata analysisMetadata = null;

		logger.debug("IN");

		try {
			super.service(request, response);

			// validate input that cames from the clien side according to rules spicified in validation.xml
			// todo: move this activity in the parent abstract class and hide input validation from actual
			// service implementation
			validateInput();

			queryName = getAttributeAsString(QbeEngineStaticVariables.CATALOGUE_NAME);
			logger.debug(QbeEngineStaticVariables.CATALOGUE_NAME + ": " + queryName);
			queryDescritpion = getAttributeAsString(QbeEngineStaticVariables.CATALOGUE_DESCRIPTION);
			logger.debug(QbeEngineStaticVariables.CATALOGUE_DESCRIPTION + ": " + queryDescritpion);
			queryScope = getAttributeAsString(QbeEngineStaticVariables.CATALOGUE_SCOPE);
			logger.debug(QbeEngineStaticVariables.CATALOGUE_SCOPE + ": " + queryScope);

			analysisMetadata = getEngineInstance().getAnalysisMetadata();
			analysisMetadata.setName(queryName);
			analysisMetadata.setDescription(queryDescritpion);

			if (EngineAnalysisMetadata.PUBLIC_SCOPE.equalsIgnoreCase(queryScope)) {
				analysisMetadata.setScope(EngineAnalysisMetadata.PUBLIC_SCOPE);
			} else if (EngineAnalysisMetadata.PRIVATE_SCOPE.equalsIgnoreCase(queryScope)) {
				analysisMetadata.setScope(EngineAnalysisMetadata.PRIVATE_SCOPE);
			} else {
				Assert.assertUnreachable("Value [" + queryScope + "] is not valid for the input parameter " + QbeEngineStaticVariables.CATALOGUE_SCOPE);
			}

			String result = saveAnalysisState();
			if (!result.trim().toLowerCase().startsWith("ok")) {
				throw new SpagoBIEngineServiceException(getActionName(), result);
			}

			try {
				String newSubobjectId = result.substring(5);
				JSONSuccess success = new JSONSuccess(newSubobjectId);
				writeBackToClient(success);
			} catch (IOException e) {
				String message = "Impossible to write back the responce to the client";
				throw new SpagoBIEngineServiceException(getActionName(), message, e);
			}

		} catch (Throwable t) {
			throw SpagoBIEngineServiceExceptionHandler.getInstance().getWrappedException(getActionName(), getEngineInstance(), t);
		} finally {
			logger.debug("OUT");
		}
	}

	public void validateInput() {
		EMFErrorHandler errorHandler = getErrorHandler();
		if (!errorHandler.isOKBySeverity(EMFErrorSeverity.ERROR)) {
			Collection errors = errorHandler.getErrors();
			Iterator it = errors.iterator();
			while (it.hasNext()) {
				EMFAbstractError error = (EMFAbstractError) it.next();
				if (error.getSeverity().equals(EMFErrorSeverity.ERROR)) {
					throw new SpagoBIEngineServiceException(getActionName(), error.getMessage(), null);
				}
			}
		}
	}
}
