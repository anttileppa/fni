package fi.foyt.fni.view.gamelibrary;

import java.util.Date;
import java.util.List;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import com.ocpsoft.pretty.faces.annotation.URLAction;
import com.ocpsoft.pretty.faces.annotation.URLMapping;
import com.ocpsoft.pretty.faces.annotation.URLMappings;
import com.ocpsoft.pretty.faces.annotation.URLQueryParameter;

import fi.foyt.fni.gamelibrary.OrderController;
import fi.foyt.fni.persistence.model.gamelibrary.Order;
import fi.foyt.fni.persistence.model.gamelibrary.OrderItem;
import fi.foyt.fni.persistence.model.gamelibrary.OrderStatus;
import fi.foyt.fni.persistence.model.users.Address;
import fi.foyt.fni.persistence.model.users.Permission;
import fi.foyt.fni.security.Secure;
import fi.foyt.fni.security.SecurityContext;
import fi.foyt.fni.security.SecurityParam;
import fi.foyt.fni.security.SecurityParams;
import fi.foyt.fni.utils.faces.FacesUtils;

@Stateful
@RequestScoped
@Named
@URLMappings(mappings = { 
		@URLMapping(id = "gamelibrary-order", 
				pattern = "/gamelibrary/orders/#{orderBackingBean.orderId}", 
				viewId = "/gamelibrary/order.jsf"
		) 
})
public class OrderBackingBean {

	@Inject
	private OrderController orderController;

	@URLAction
	@Secure (Permission.GAMELIBRARY_VIEW_ORDER)
	@SecurityContext (context = "#{orderBackingBean.orderId}")
	@SecurityParams (
    @SecurityParam (name="accessKey", value="#{orderBackingBean.accessKey}")
	)
	public void init() {
		Order order = orderController.findOrderById(getOrderId());
		orderStatus = order.getOrderStatus();
		customerCompany = order.getCustomerCompany();
		customerFirstName = order.getCustomerFirstName();
		customerLastName = order.getCustomerLastName();
		customerEmail = order.getCustomerEmail();
		customerPhone = order.getCustomerPhone();
		customerMobile = order.getCustomerMobile();
  	deliveryAddress = order.getDeliveryAddress();
  	orderItems = orderController.listOrderItems(order);
  	notes = order.getNotes();
		created = order.getCreated();
	  paid = order.getPaid();
	  shipped = order.getShipped();
	  delivered = order.getDelivered();
	  canceled = order.getCanceled(); 
	}

	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}

	public OrderStatus getOrderStatus() {
		return orderStatus;
	}
	
	public String getOrderStatusText() {
		switch (orderStatus) {
  		case NEW:
  			return FacesUtils.getLocalizedValue("gamelibrary.order.statusWaitingPayment");
  		case CANCELED:
  			return FacesUtils.getLocalizedValue("gamelibrary.order.statusCanceled");
  		case DELIVERED:
  			return FacesUtils.getLocalizedValue("gamelibrary.order.statusDelivered");
  		case SHIPPED:
  			return FacesUtils.getLocalizedValue("gamelibrary.order.statusShipped");
  		case WAITING_FOR_DELIVERY:
  			return FacesUtils.getLocalizedValue("gamelibrary.order.statusWaitingDelivery");
  		case PAID:
  			return FacesUtils.getLocalizedValue("gamelibrary.order.statusPaid");
  	}

		return "";
	}

	public Address getDeliveryAddress() {
		return deliveryAddress;
	}

	public String getCustomerCompany() {
		return customerCompany;
	}

	public String getCustomerFirstName() {
		return customerFirstName;
	}

	public String getCustomerLastName() {
		return customerLastName;
	}

	public String getCustomerEmail() {
		return customerEmail;
	}

	public String getCustomerPhone() {
		return customerPhone;
	}

	public String getCustomerMobile() {
		return customerMobile;
	}

	public List<OrderItem> getOrderItems() {
		return orderItems;
	}

  public String getNotes() {
		return notes;
	}

	public Date getCreated() {
		return created;
	}
	
	public Date getPaid() {
		return paid;
	}
	
	public Date getShipped() {
		return shipped;
	}
	
	public void setShipped(Date shipped) {
		this.shipped = shipped;
	}
	
	public Date getDelivered() {
		return delivered;
	}
	
	public Date getCanceled() {
		return canceled;
	}
	
	public String getAccessKey() {
    return accessKey;
  }
	
	public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  private Long orderId;
	private OrderStatus orderStatus;
	private String customerCompany;
	private String customerFirstName;
	private String customerLastName;
	private String customerEmail;
	private String customerPhone;
	private String customerMobile;
	private Address deliveryAddress;
  private List<OrderItem> orderItems;
	private String notes;
  private Date created;
  private Date paid;
  private Date shipped;
  private Date delivered;
  private Date canceled; 
  
  @URLQueryParameter ("key") 
  private String accessKey;
}
