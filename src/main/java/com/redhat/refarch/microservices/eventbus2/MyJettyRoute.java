/*
 * Copyright 2005-2015 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.redhat.refarch.microservices.eventbus2;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.model.rest.RestBindingMode;

/**
 * Configures all our Camel routes, components, endpoints and beans
 */
@ContextName("EventBus2")
public class MyJettyRoute extends RouteBuilder {
	
	private static final String ACTION_CREATE = "create";
	
	private static final String ACTION_DELETE = "delete";
	
	private static final String ACTION_INFO = "info";

    @Override
    public void configure() throws Exception {
        
    	String host = System.getenv("AMQ_HOST");
    	if (host == null) {
    		host = "0.0.0.0";
    	}
    	String port = System.getenv("AMQ_PORT");
    	if (port == null) {
    		port = "61616";
    	}
    	String username = System.getenv("AMQ_USERNAME");
    	if (username == null) {
    		username = "admin";
    	}
    	String password = System.getenv("AMQ_PASSWORD");
    	if (password == null) {
    		password = "manager1";
    	}
    	
    	ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(username, password, "tcp://" + host + ":" + port);
    	getContext().addComponent("jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));
    	
    	
    	
    	restConfiguration().component("jetty").host("0.0.0.0").port(8080).bindingMode(RestBindingMode.auto);
    	
    	
    	
    	rest("/v3/event/order/")
        	.get("/{id}").to("direct:processNewOrderEvent")
        	.post("/{id}").to("direct:processInfoOrderEvent")
        	.delete("/{id}").to("direct:processDeleteOrderEvent");
    	
    	
    	
    	rest("/v3/event/customer/")
    		.get("/{id}").to("direct:processNewCustomerEvent")
        	.post("/{id}").to("direct:processInfoCustomerEvent")
        	.delete("/{id}").to("direct:processDeleteCustomerEvent");

    	
    	
    	from("direct:processNewOrderEvent").log(LoggingLevel.INFO, "New order ${header.id} event received").setHeader("type", constant(ACTION_CREATE)).inOnly("jms:queue:activemq/queue/orders");
        
        from("direct:processInfoOrderEvent").log(LoggingLevel.INFO, "Order info ${header.id} event received").setHeader("type", constant(ACTION_INFO)).inOnly("jms:queue:activemq/queue/orders");
        
        from("direct:processDeleteOrderEvent").log(LoggingLevel.INFO, "Delete order ${header.id} event received").setHeader("type", constant(ACTION_DELETE)).inOnly("jms:queue:activemq/queue/orders");   
        
        from("jms:queue:activemq/queue/orders?selector='" + ACTION_CREATE + "'").log("Created order ${header.id} event processed").log("Booked SKUs in inventory");
        
        from("jms:queue:activemq/queue/orders?selector='" + ACTION_DELETE + "'").log("Delete order ${header.id} event processed").log("SKUs in inventory released");
        
        from("jms:queue:activemq/queue/orders?selector='" + ACTION_INFO + "'").log("Order info ${header.id} event processed").log("Order info provided");
    
        
        
        from("direct:processNewCustomerEvent").log(LoggingLevel.INFO, "New customer ${header.id} event received").setHeader("type", constant(ACTION_CREATE)).inOnly("jms:queue:activemq/queue/customers");
        
        from("direct:processInfoCustomerEvent").log(LoggingLevel.INFO, "Customer info ${header.id} event received").setHeader("type", constant(ACTION_INFO)).inOnly("jms:queue:activemq/queue/customers");
        
        from("direct:processDeleteCustomerEvent").log(LoggingLevel.INFO, "Delete customer ${header.id} event received").setHeader("type", constant(ACTION_DELETE)).inOnly("jms:queue:activemq/queue/customers");   
        
        from("jms:queue:activemq/queue/customers?selector='" + ACTION_CREATE + "'").log("Created customer ${header.id} event processed");
        
        from("jms:queue:activemq/queue/customers?selector='" + ACTION_DELETE + "'").log("Delete customer ${header.id} event processed");
        
        from("jms:queue:activemq/queue/customers?selector='" + ACTION_INFO + "'").log("Customer info ${header.id} event processed");
       
    }
}
