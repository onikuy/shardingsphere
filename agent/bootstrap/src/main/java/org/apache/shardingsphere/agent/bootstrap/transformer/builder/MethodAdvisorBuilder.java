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

package org.apache.shardingsphere.agent.bootstrap.transformer.builder;

import lombok.RequiredArgsConstructor;
import net.bytebuddy.description.method.MethodDescription.InDefinedShape;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import org.apache.shardingsphere.agent.advice.AgentAdvice;
import org.apache.shardingsphere.agent.advice.type.ConstructorAdvice;
import org.apache.shardingsphere.agent.advice.type.InstanceMethodAdvice;
import org.apache.shardingsphere.agent.advice.type.StaticMethodAdvice;
import org.apache.shardingsphere.agent.bootstrap.logging.LoggerFactory;
import org.apache.shardingsphere.agent.bootstrap.logging.LoggerFactory.Logger;
import org.apache.shardingsphere.agent.bootstrap.plugin.executor.type.ConstructorAdviceExecutor;
import org.apache.shardingsphere.agent.bootstrap.plugin.executor.type.InstanceMethodAdviceExecutor;
import org.apache.shardingsphere.agent.bootstrap.plugin.executor.type.StaticMethodAdviceExecutor;
import org.apache.shardingsphere.agent.bootstrap.transformer.MethodAdvisor;
import org.apache.shardingsphere.agent.config.advisor.AdvisorConfiguration;
import org.apache.shardingsphere.agent.bootstrap.transformer.builder.advise.AdviceFactory;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Method advisor builder.
 */
@RequiredArgsConstructor
public final class MethodAdvisorBuilder {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodAdvisorBuilder.class);
    
    private final AdviceFactory adviceFactory;
    
    private final AdvisorConfiguration advisorConfig;
    
    private final TypeDescription typePointcut;
    
    /**
     * Build method advisor builder.
     * 
     * @param builder original builder
     * @return built builder
     */
    public Builder<?> build(final Builder<?> builder) {
        Builder<?> result = builder;
        for (MethodAdvisor each : getMatchedMethodAdvisors()) {
            try {
                result = each.getAdviceExecutor().decorateBuilder(result, each.getPointcut());
                // CHECKSTYLE:OFF
            } catch (final Throwable ex) {
                // CHECKSTYLE:ON
                LOGGER.error("Failed to load advice class: {}.", typePointcut.getTypeName(), ex);
            }
        }
        return result;
    }
    
    private Collection<MethodAdvisor> getMatchedMethodAdvisors() {
        return typePointcut.getDeclaredMethods().stream().map(this::findMatchedMethodAdvisor).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }
    
    private Optional<MethodAdvisor> findMatchedMethodAdvisor(final InDefinedShape methodDescription) {
        Collection<AgentAdvice> advices = advisorConfig.getAdvisors().stream()
                .filter(each -> each.getPointcut().matches(methodDescription)).map(each -> adviceFactory.getAdvice(each.getAdviceClassName())).collect(Collectors.toList());
        if (isConstructor(methodDescription)) {
            return Optional.of(new MethodAdvisor(methodDescription, new ConstructorAdviceExecutor(advices.stream().map(each -> (ConstructorAdvice) each).collect(Collectors.toList()))));
        }
        if (isStaticMethod(methodDescription)) {
            return Optional.of(new MethodAdvisor(methodDescription, new StaticMethodAdviceExecutor(advices.stream().map(each -> (StaticMethodAdvice) each).collect(Collectors.toList()))));
        }
        if (isMethod(methodDescription)) {
            return Optional.of(new MethodAdvisor(methodDescription, new InstanceMethodAdviceExecutor(advices.stream().map(each -> (InstanceMethodAdvice) each).collect(Collectors.toList()))));
        }
        return Optional.empty();
    }
    
    private boolean isConstructor(final InDefinedShape methodDescription) {
        return methodDescription.isConstructor();
    }
    
    private boolean isStaticMethod(final InDefinedShape methodDescription) {
        return methodDescription.isStatic() && isMethod(methodDescription);
    }
    
    private boolean isMethod(final InDefinedShape methodDescription) {
        return !(methodDescription.isAbstract() || methodDescription.isSynthetic());
    }
}
