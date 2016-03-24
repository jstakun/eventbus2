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

import javax.inject.Inject;

import org.apache.camel.Endpoint;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.cdi.Uri;
import org.apache.camel.model.rest.RestBindingMode;

/**
 * Configures all our Camel routes, components, endpoints and beans
 */
@ContextName("EventBus2")
public class MyJettyRoute extends RouteBuilder {
	
	private static final String ACTION_CREATE = "create";
	
	private static final String DELETE_CREATE = "delete";
	
	private static final String INFO_CREATE = "info";
	
	@Inject @Uri("activemq:queue:orders") 
    private Endpoint amqOrdersQueueEndpoint; 
	
	@Inject @Uri("activemq:queue:orders?selector=type='" + ACTION_CREATE +"'") 
    private Endpoint amqOrdersCreateQueueEndpoint; 
	
	@Inject @Uri("activemq:queue:orders?selector=type='" + DELETE_CREATE +"'") 
    private Endpoint amqOrdersDeleteQueueEndpoint; 
	
	@Inject @Uri("activemq:queue:orders?selector=type='" + INFO_CREATE +"'") 
    private Endpoint amqOrdersInfoQueueEndpoint; 
	
    @Override
    public void configure() throws Exception {
        
    	restConfiguration().component("jetty").host("0.0.0.0").port(8080).bindingMode(RestBindingMode.auto);
    	
    	rest("/v3/event/order/")
        	.get("/{id}").to("direct:processNewOrderEvent")
        	.post("/{id}").to("direct:processInfoOrderEvent")
        	.delete("/{id}").to("direct:processDeleteOrderEvent");

    	from("direct:processNewOrderEvent").log(LoggingLevel.INFO, "New order ${header.id} event received").setHeader("type", constant(ACTION_CREATE)).to(amqOrdersQueueEndpoint);
        
        from("direct:processInfoOrderEvent").log(LoggingLevel.INFO, "Order info ${header.id} event received").setHeader("type", constant(INFO_CREATE)).to(amqOrdersQueueEndpoint);
        
        from("direct:processDeleteOrderEvent").log(LoggingLevel.INFO, "Delete order ${header.id} event received").setHeader("type", constant(DELETE_CREATE)).to(amqOrdersQueueEndpoint);   
        
        from("amqOrdersCreateQueueEndpoint").log("Created order ${header.id} event processed").log("Booked SKUs in inventory");
        
        from("amqOrdersDeleteQueueEndpoint").log("Delete order ${header.id} event processed").log("SKUs in inventory released");
        
        from("amqOrdersInfoQueueEndpoint").log("Order info ${header.id} event processed").log("Order info provided");
    }

}
