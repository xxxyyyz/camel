/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.disruptor;

import org.apache.camel.FailedToStartRouteException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spi.RouteController;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DisruptorConcurrentConsumersNPEIssueTest extends CamelTestSupport {
    @Test
    void testSendToDisruptor() throws Exception {
        final MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceived("Hello World");

        template.sendBody("disruptor:foo", "Hello World");

        MockEndpoint.assertIsSatisfied(context);
        RouteController routeController = context.getRouteController();

        Exception ex = assertThrows(FailedToStartRouteException.class,
                () -> routeController.startRoute("first"));

        assertEquals("Failed to start route: first because: Multiple consumers for the same endpoint is not "
                     + "allowed: disruptor://foo?concurrentConsumers=5",
                ex.getMessage());
    }

    @Test
    void testStartThird() throws Exception {
        final MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceived("Hello World");

        template.sendBody("disruptor:foo", "Hello World");

        MockEndpoint.assertIsSatisfied(context);

        // this should be okay
        context.getRouteController().startRoute("third");

        RouteController routeController = context.getRouteController();

        Exception ex = assertThrows(FailedToStartRouteException.class,
                () -> routeController.startRoute("first"));

        assertEquals("Failed to start route: first because: Multiple consumers for the same endpoint is not allowed:"
                     + " disruptor://foo?concurrentConsumers=5",
                ex.getMessage());
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("disruptor:foo?concurrentConsumers=5").routeId("first").noAutoStartup()
                        .to("mock:result");

                from("disruptor:foo?concurrentConsumers=5").routeId("second").to("mock:result");

                from("direct:foo").routeId("third").noAutoStartup().to("mock:result");
            }
        };
    }
}
