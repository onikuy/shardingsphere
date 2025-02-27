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

package org.apache.shardingsphere.agent.bootstrap.plugin.executor.type;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.shardingsphere.agent.advice.type.StaticMethodAdvice;
import org.apache.shardingsphere.agent.bootstrap.logging.LoggerFactory;
import org.apache.shardingsphere.agent.bootstrap.logging.LoggerFactory.Logger;
import org.apache.shardingsphere.agent.bootstrap.plugin.PluginContext;
import org.apache.shardingsphere.agent.bootstrap.plugin.executor.AdviceExecutor;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Static method advice executor.
 */
@RequiredArgsConstructor
public final class StaticMethodAdviceExecutor implements AdviceExecutor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticMethodAdviceExecutor.class);
    
    private final Collection<StaticMethodAdvice> executors;
    
    /**
     * Intercept static method.
     *
     * @param klass target class
     * @param method intercepted method
     * @param args all arguments of method
     * @param callable origin method invocation
     * @return return value of target invocation
     */
    @RuntimeType
    @SneakyThrows
    public Object intercept(@Origin final Class<?> klass, @Origin final Method method, @AllArguments final Object[] args, @SuperCall final Callable<?> callable) {
        boolean adviceEnabled = PluginContext.isPluginEnabled();
        if (adviceEnabled) {
            interceptBefore(klass, method, args);
        }
        Object result = null;
        try {
            result = callable.call();
            // CHECKSTYLE:OFF
        } catch (final Throwable ex) {
            // CHECKSTYLE:ON
            if (adviceEnabled) {
                interceptThrow(klass, method, args, ex);
            }
            throw ex;
        } finally {
            if (adviceEnabled) {
                interceptAfter(klass, method, args, result);
            }
        }
        return result;
    }
    
    private void interceptBefore(final Class<?> klass, final Method method, final Object[] args) {
        try {
            for (StaticMethodAdvice each : executors) {
                each.beforeMethod(klass, method, args);
            }
            // CHECKSTYLE:OFF
        } catch (final Throwable ex) {
            // CHECKSTYLE:ON
            LOGGER.error("Failed to execute the pre-method of method `{}` in class `{}`.", method.getName(), klass, ex);
        }
    }
    
    private void interceptThrow(final Class<?> klass, final Method method, final Object[] args, final Throwable ex) {
        try {
            for (StaticMethodAdvice each : executors) {
                each.onThrowing(klass, method, args, ex);
            }
            // CHECKSTYLE:OFF
        } catch (final Throwable ignored) {
            // CHECKSTYLE:ON
            LOGGER.error("Failed to execute the error handler of method `{}` in class `{}`.", method.getName(), klass, ex);
        }
    }
    
    private void interceptAfter(final Class<?> klass, final Method method, final Object[] args, final Object result) {
        try {
            for (StaticMethodAdvice each : executors) {
                each.afterMethod(klass, method, args, result);
            }
            // CHECKSTYLE:OFF
        } catch (final Throwable ex) {
            // CHECKSTYLE:ON
            LOGGER.error("Failed to execute the post-method of method `{}` in class `{}`.", method.getName(), klass, ex);
        }
    }
    
    @Override
    public Builder<?> decorateBuilder(final Builder<?> builder, final MethodDescription pointcut) {
        return builder.method(ElementMatchers.is(pointcut)).intercept(MethodDelegation.withDefaultConfiguration().to(this));
    }
}
