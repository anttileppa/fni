package fi.foyt.fni.view.users;

import java.io.IOException;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.codec.digest.DigestUtils;

import com.ocpsoft.pretty.faces.annotation.URLMapping;
import com.ocpsoft.pretty.faces.annotation.URLMappings;

import fi.foyt.fni.auth.AuthenticationController;
import fi.foyt.fni.persistence.model.users.PasswordResetKey;
import fi.foyt.fni.utils.faces.FacesUtils;

@Named
@RequestScoped
@Stateful
@URLMappings(mappings = {
  @URLMapping(
		id = "users-password-reset", 
		pattern = "/users/resetpassword/#{passwordResetBackingBean.key}", 
		viewId = "/users/resetpassword.jsf"
  )
})
public class PasswordResetBackingBean {

	@Inject
	private AuthenticationController authenticationController;
	
	public String getKey() {
		return key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	public String getPassword1() {
		return password1;
	}
	
	public void setPassword1(String password1) {
		this.password1 = password1;
	}
	
	public String getPassword2() {
		return password2;
	}
	
	public void setPassword2(String password2) {
		this.password2 = password2;
	}
	
	public void changePassword() throws IOException {
		if (!getPassword1().equals(getPassword2())) {
			FacesUtils.addMessage(FacesMessage.SEVERITY_WARN, FacesUtils.getLocalizedValue("users.resetPassword.passwordsDontMatch"));
		} else {
			if (DigestUtils.md5Hex("").equals(getPassword1())) {
				FacesUtils.addMessage(FacesMessage.SEVERITY_WARN, FacesUtils.getLocalizedValue("users.resetPassword.passwordRequired"));
			} else {
    		PasswordResetKey passwordResetKey = authenticationController.findPasswordResetKey(getKey());
    		if (passwordResetKey != null) {
      		authenticationController.setUserPassword(passwordResetKey.getUser(), getPassword1());
      		authenticationController.deletePasswordResetKey(passwordResetKey);
      		ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
      		externalContext.redirect(new StringBuilder()
        	  .append(externalContext.getRequestContextPath())
        	  .append("/login/")
        	  .toString());  
    		} else {
    			FacesUtils.addMessage(FacesMessage.SEVERITY_ERROR, FacesUtils.getLocalizedValue("users.resetPassword.invalidPasswordResetKey"));
    		}
			}
		}
	}
	
	private String key;
	private String password1;
	private String password2;
}
