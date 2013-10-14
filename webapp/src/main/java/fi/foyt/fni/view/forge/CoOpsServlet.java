package fi.foyt.fni.view.forge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.codehaus.jackson.map.ObjectMapper;

import fi.foyt.fni.coops.model.File;
import fi.foyt.fni.coops.model.Join;
import fi.foyt.fni.coops.model.Patch;
import fi.foyt.fni.materials.DocumentController;
import fi.foyt.fni.persistence.model.materials.Document;
import fi.foyt.fni.persistence.model.materials.DocumentRevision;
import fi.foyt.fni.session.SessionController;
import fi.foyt.fni.utils.compression.CompressionUtils;
import fi.foyt.fni.utils.diff.DiffUtils;
import fi.foyt.fni.utils.diff.PatchResult;
import fi.foyt.fni.view.AbstractTransactionedServlet;

@WebServlet(urlPatterns = "/forge/coops/*")
public class CoOpsServlet extends AbstractTransactionedServlet {

	private static final long serialVersionUID = -1L;

	private final static String COOPS_PROTOCOL_VERSION = "1.0.0draft2";
	private final static String[] COOPS_SUPPORTED_ALGORITHMS = { "dmp" };
	private final static String[] COOPS_SUPPORTED_EXTENSIONS = { "x-http-method-override" };
	private final static String COOPS_DOCUMENT_CONTENTTYPE = "text/html;editor=CKEditor";

	@Inject
	private DocumentController documentController;

	@Inject
	private SessionController sessionController;
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO: Security
		
		String pathInfo = StringUtils.removeStart(request.getPathInfo(), "/");
		String[] path = pathInfo.split("/");
		if (path.length < 1) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid request");
			return;
		}
 		
		if (!StringUtils.isNumeric(path[0])) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid request");
			return;
		}
		
		Long materialId = NumberUtils.createLong(path[0]);
		Document document = documentController.findDocumentById(materialId);
		
		if (document == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found");
			return;
		}
		
		if (path.length == 2 && "join".equals(path[1])) {
			String[] algorithms = request.getParameterValues("algorithm");
			String protocolVersion = request.getParameter("protocolVersion");
			
			if ((algorithms == null)||(algorithms.length == 0)) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid request");
				return;
			}
			
			if (StringUtils.isBlank(protocolVersion)) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid request");
				return;
			}
			
			handleJoinRequest(request, response, protocolVersion, algorithms, document);
		} else if (path.length == 2 && "update".equals(path[1])) {
			Long revisionNumber = NumberUtils.createLong(request.getParameter("revisionNumber"));
			if (revisionNumber == null) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid request");
				return;
			}
			
			handleUpdateRequest(request, response, document, revisionNumber);
		} else if (path.length == 1) {
			Long revisionNumber = NumberUtils.createLong(request.getParameter("revisionNumber"));
			handleFileRequest(request, response, document, revisionNumber);
		}
	}
	
	private void handleFileRequest(HttpServletRequest request, HttpServletResponse response, Document document, Long revisionNumber) throws IOException {
		if (revisionNumber != null) {
			response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "This feature is not implemented yet");
			return;
		}
		
		String content = new String(document.getData(), "UTF-8");
		writeJsonResponse(response, new File(document.getId().toString(), document.getTitle(), document.getModified(), revisionNumber, content, COOPS_DOCUMENT_CONTENTTYPE));
	}

	@Override
	protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String pathInfo = StringUtils.removeStart(request.getPathInfo(), "/");
		String[] path = pathInfo.split("/");
		if (path.length < 1) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid request");
			return;
		}
 		
		if (!StringUtils.isNumeric(path[0])) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid request");
			return;
		}
		
		Long materialId = NumberUtils.createLong(path[0]);
		Document document = documentController.findDocumentById(materialId);
		
		if (document == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found");
			return;
		}
		
		Patch patch = null;
		ServletInputStream inputStream = request.getInputStream();
		
		try {
		  ObjectMapper objectMapper = new ObjectMapper();
		  patch = objectMapper.readValue(inputStream, Patch.class);
		} finally {
			inputStream.close();
		}
		
		if (patch == null) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid request");
			return;
		}
		
		if (patch.getRevisionNumber() == null) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid request");
			return;
		}

		handlePatchRequest(request, response, document, patch.getRevisionNumber(), patch.getPatch());
	}
	
	protected void handlePatchRequest(HttpServletRequest request, HttpServletResponse response, Document document, Long clientRevisionNumber, String patch) throws IOException {
		Long revisionNumber = documentController.getDocumentRevision(document);
		if (!revisionNumber.equals(clientRevisionNumber)) {
			response.sendError(HttpServletResponse.SC_CONFLICT, "Out of sync");
			return;
		} 
		
		byte[] patchData = null;
		String checksum = null;
		if (StringUtils.isNotBlank(patch)) {
	    String oldData = new String(document.getData(), "UTF-8");
      
      PatchResult patchResult = DiffUtils.applyPatch(oldData, patch);
      if (!patchResult.allApplied()) {
      	response.sendError(HttpServletResponse.SC_CONFLICT, "Patching failed");
  			return;
      }
      
      String data = patchResult.getPatchedText();
      documentController.updateDocumentData(document, data, sessionController.getLoggedUser());
      checksum = DigestUtils.md5Hex(data);
      patchData = patch.getBytes("UTF-8");
		}

		Long patchRevisionNumber = revisionNumber + 1;
    documentController.createDocumentRevision(document, patchRevisionNumber, new Date(), false, false, patchData, checksum);
    
    writeJsonResponse(response, new Patch(patchRevisionNumber, null, null));
	}

	private void handleUpdateRequest(HttpServletRequest request, HttpServletResponse response, Document document, Long clientRevisionNumber) throws IOException {
		List<DocumentRevision> documentRevisions = documentController.listDocumentRevisionsAfter(document, clientRevisionNumber);
		List<Patch> updateResults = new ArrayList<>();
		
		if (documentRevisions.isEmpty()) {
    	response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return;
		}
		
		for (DocumentRevision documentRevision : documentRevisions) {
			String patch = null;
      byte[] patchData = documentRevision.getData();
      if (patchData != null) {
        if (documentRevision.getCompressed()) {
          patchData = CompressionUtils.uncompressBzip2Array(patchData);
        }

        patch = new String(patchData, "UTF-8");
      }
      
      if (patch != null) {
      	updateResults.add(new Patch(documentRevision.getRevision(), patch, documentRevision.getChecksum()));
      } else {
      	updateResults.add(new Patch(documentRevision.getRevision(), null, null));
      }
    }

		writeJsonResponse(response, updateResults);
	}

	private void handleJoinRequest(HttpServletRequest request, HttpServletResponse response, String clientProtocolVersion, String[] clientAlgorithms, Document document) throws IOException {
		// TODO: WebSocket support when available in Application Container

		if (!clientProtocolVersion.equals(COOPS_PROTOCOL_VERSION)) {
			response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "Protocol version mismatch. Client is using " + clientProtocolVersion + " and server " + COOPS_PROTOCOL_VERSION);
			return;
		}
		
		String algorithm = null;
		for (String clientAlgorithm : clientAlgorithms) {
			if (ArrayUtils.contains(COOPS_SUPPORTED_ALGORITHMS, clientAlgorithm)) {
				algorithm = clientAlgorithm;
				break;
			}
		}
		
		if (algorithm == null) {
			response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, 
				"Server and client do not have a commonly supported algorithm. " + 
			  "Server supported: " + StringUtils.join(COOPS_SUPPORTED_ALGORITHMS, ',') + ", " + 
				"Client supported: " + StringUtils.join(clientAlgorithms, ','));
			return;
		}

		Long revisionNumber = documentController.getDocumentRevision(document);
		writeJsonResponse(response, new Join(COOPS_SUPPORTED_EXTENSIONS, revisionNumber, new String(document.getData(), "UTF-8"), COOPS_DOCUMENT_CONTENTTYPE, UUID.randomUUID().toString()));
	}
	
	private void writeJsonResponse(HttpServletResponse response, Object object) throws IOException {
		ServletOutputStream outputStream = response.getOutputStream();
		try {
  		response.setContentType("application/json; charset=utf-8");
  		response.setStatus(HttpServletResponse.SC_OK);
  		
			ObjectMapper objectMapper = new ObjectMapper();
  		objectMapper.writeValue(outputStream, object);
		} finally {
			outputStream.flush();
			outputStream.close();
		}
	}
}