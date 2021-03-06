package fi.foyt.fni.view;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.apache.commons.lang3.StringUtils;

public class AbstractTransactionedServlet extends HttpServlet {
	
	private static final String METHOD_PATCH = "PATCH";
	
	private static final long serialVersionUID = -1673932340848238141L;

	@Resource
	private UserTransaction userTransaction;

	@Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
  	try {
    	// If transaction is not already active, we start it
   		boolean transactionActive = userTransaction.getStatus() == Status.STATUS_ACTIVE;
   		if (!transactionActive) {
   			userTransaction.begin();
   		}
  
  		try {
     		// Proceed with the request

  			String method = req.getMethod();
  			String methodOverride = req.getHeader("x-http-method-override");
  			if (StringUtils.isNotBlank(methodOverride)) {
  				method = methodOverride;
  			}
  			
				if (METHOD_PATCH.equalsIgnoreCase(method)) {
					doPatch(req, resp);
				} else {
		  		super.service(req, resp);
				}
    
    		// If transaction was started here, we commit the transaction
    		if (!transactionActive) {
    			userTransaction.commit();
    		} 
    		
    	} catch (Throwable t) {
    		// If exception was thrown and the transaction was started here, we rollback the transaction
    		if (!transactionActive) {
    			userTransaction.rollback();
    		}
    
    		// ... and throw an IOException
    		throw new IOException(t);
    	}  	
  	} catch (SystemException | NotSupportedException e) {
  		throw new ServletException(e);
		}
  }

	protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Not implemented");
	}

}
