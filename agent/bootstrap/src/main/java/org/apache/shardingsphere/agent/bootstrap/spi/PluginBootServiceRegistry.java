/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.agent.bootstrap.spi;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.agent.spi.PluginBootService;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 *  Plugin boot service registry.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PluginBootServiceRegistry {
    
    /**
     * Get registered service.
     *
     * @param type type
     * @return registered service
     */
    public static Optional<PluginBootService> getRegisteredService(final String type) {
        return AgentServiceLoader.getServiceLoader(PluginBootService.class).getServices().stream().filter(each -> each.getType().equalsIgnoreCase(type)).findFirst();
    }
    
    /**
     * Get all registered services.
     *
     * @return registered services
     */
    public static Collection<PluginBootService> getAllRegisteredServices() {
        return AgentServiceLoader.getServiceLoader(PluginBootService.class).getServices();
    }
    
    /**
     * Create new instances.
     *
     * @param classLoader class loader
     * @return created instances
     */
    public static Collection<PluginBootService> newInstances(final ClassLoader classLoader) {
        Collection<PluginBootService> result = new LinkedList<>();
        for (PluginBootService each : ServiceLoader.load(PluginBootService.class, classLoader)) {
            result.add(each);
        }
        return result;
    }
}
